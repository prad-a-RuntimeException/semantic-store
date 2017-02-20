package recipestore.graph

import com.twitter.storehaus.cache.MapCache.empty
import com.twitter.storehaus.cache._
import org.apache.jena.rdf.model.Resource
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.graphframes.GraphFrame
import org.slf4j.{Logger, LoggerFactory}
import recipestore.graph.DataTypeHelper.{getValue, inferField}
import recipestore.graph.GraphVisitor.{Edge, Vertex}
import recipestore.metrics.{MeterWrapper, MetricsFactory, MetricsWrapper}

import scala.collection.JavaConverters._
import scala.collection.immutable.{Iterable, Seq}
import scala.collection.mutable


object PropertyGraphFactory {


  val LOGGER: Logger = LoggerFactory.getLogger(PropertyGraphFactory.getClass)

  private val vertexCache: MutableCache[Resource, (mutable.Iterable[VertexValue], mutable.Iterable[Row])] =
    empty[Resource, (mutable.Iterable[VertexValue], mutable.Iterable[Row])].toMutable()

  val memoizedCreateVertexFunction = Memoize(vertexCache)(createVertices)
  private val schemaCache: MutableCache[String, StructType] =
    empty[String, StructType].toMutable()
  val sparkSession: SparkSession = SparkSession.builder()
    .appName("GraphFactory")
    .master("local")
    .config("spark.driver.memory", "2g")
    .config("spark.executor.memory", "4g")
    .config("SPARK_CONF_DIR", "./infrastructure/spark-config")
    .getOrCreate()
  val sqlContext: SQLContext = sparkSession.sqlContext

  val edgeSchema: StructType = StructType(Array("src", "dst", "relationship")
    .map(edgeVal => StructField.apply(edgeVal, StringType)))

  def vertexAggregator(vertex1: DataFrame, vertex2: DataFrame) = {
    mergeColumns(vertex1, vertex2)
  }

  var meter: MetricsWrapper = null

  def createGraph(resources: Stream[Resource], limit: Int = -1): GraphFrame = {

    meter = MetricsFactory.get("CreatePropertyGraph", classOf[MeterWrapper])
    val verticesAndEdges: Seq[(mutable.Iterable[VertexValue], mutable.Iterable[Row])] =
      (if (limit > 0) resources.take(limit) else resources)
        .map(resource => memoizedCreateVertexFunction(resource))

    val vertexMeter = MetricsFactory.get("VertexMeter", classOf[MeterWrapper])
    val verticesDF: DataFrame = verticesAndEdges
      .flatMap(_._1)
      .groupBy(_.schema)
      .map(rowsForSchema => {
        vertexMeter.poke
        try {
          sqlContext.createDataFrame(rowsForSchema._2.map(_.row).toList.asJava, rowsForSchema._1)
        } catch {
          case e: Throwable => LOGGER.error("Failed to create dataframe ", e)
            return null;
        }
      })
      .filter(_ != null)
      .reduce((mem, curr) => mergeColumns(mem, curr))
    LOGGER.info("Done loading vertex ")

    MetricsFactory.remove("VertexMeter", classOf[MeterWrapper])

    val edgesDF: Seq[Row] = verticesAndEdges
      .flatMap(_._2)

    MetricsFactory.remove("CreatePropertyGraph", classOf[MeterWrapper])
    LOGGER.info("Done loading edge ")
    GraphFrame(verticesDF, sqlContext.createDataFrame(edgesDF.toList.asJava, edgeSchema))
  }

  def createGraph(resource: Resource): GraphFrame = {

    val dataFrames: (mutable.Iterable[VertexValue], mutable.Iterable[Row]) = memoizedCreateVertexFunction(resource)
    GraphFrame(dataFrames._1
      .groupBy(_.schema)
      .map(rowsForSchema => {
        try {
          sqlContext.createDataFrame(rowsForSchema._2.map(_.row).toList.asJava, rowsForSchema._1)
        } catch {
          case e: Throwable => LOGGER.error("Failed to create dataframe ", e)
            return null;
        }
      })
      .filter(_ != null)
      .reduce((mem, curr) => mergeColumns(mem, curr)), sqlContext.createDataFrame(dataFrames._2.toList.asJava, edgeSchema))
  }

  def getResourceID(resource: Resource): String = {
    if (resource.isAnon)
      resource.getId.toString
    else
      resource.getURI

  }

  class VertexValue(val schema: StructType, val row: Row) {

  }


  def createVertices(resource: Resource): (mutable.Iterable[VertexValue], mutable.Iterable[Row]) = {

    if (meter != null) meter.poke
    val edgeDataFrames: mutable.ListBuffer[Row] = mutable.ListBuffer()
    var vertexValues: mutable.ListBuffer[VertexValue] = mutable.ListBuffer()

    def createVertex(vertex: Vertex) = {
      val schema: StructType = getSchema(vertex)
      val propertyMap: Map[String, Object] = vertex.properties
        .+("id" -> vertex.id)
        .+("type" -> vertex.typeVal)
      val propertyValues: List[Any] = schema.map(structField => getValue(structField,
        propertyMap.getOrElse(structField.name, null))).toList

      if (propertyValues.size > 0)
        vertexValues.+=(new VertexValue(schema, Row.fromSeq(propertyValues)))
      else {
        LOGGER.warn("Dataframe found with no valid property {}", propertyMap)
        null
      }
    }

    def createEdge(edge: Edge) = {
      val edgeRow: Row = Row.fromSeq(Array(edge.outVertex, edge.inVertex, edge.property)
        .map(edgeVal => edgeVal).toSeq)
      edgeDataFrames.+=(edgeRow)
    }

    GraphVisitor.traverse(resource, createVertex, createEdge)

    (vertexValues, edgeDataFrames)

  }

  private def getSchema(vertex: Vertex): StructType = {
    def getStructType(): StructType = {
      val structFields: Iterable[StructField] = vertex.properties
        .map(property => StructField(property._1, inferField(property._2), true))

      StructType(structFields.toSeq
        .+:(StructField.apply("id", StringType, false))
        .+:(StructField.apply("type", StringType, true)))
    }

    if (vertex.typeVal != null) {
      schemaCache.getOrElseUpdate(vertex.typeVal, getStructType)
    } else
      getStructType

  }

  def mergeColumns(df1: DataFrame, df2: DataFrame): DataFrame = {

    val cols1 = df1.columns.toSet
    val cols2 = df2.columns.toSet
    val total = cols1 ++ cols2 // union

    def unionOfColumns(myCols: Set[String], allCols: Set[String]) = {
      allCols.toList.map(x => x match {
        case x if myCols.contains(x) => col(x)
        case _ => lit(null).as(x)
      })
    }

    df1.select(unionOfColumns(cols1, total): _*).union(df2.select(unionOfColumns(cols2, total): _*))

  }

}
