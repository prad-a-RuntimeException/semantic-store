package recipestore.graph

import org.apache.jena.rdf.model.{Literal, Property, RDFNode, Resource, Statement}
import org.apache.spark.sql._
import org.apache.spark.sql.types.{StructField, StructType}
import org.graphframes.GraphFrame
import recipestore.graph.DataTypeHelper.inferField

import scala.collection.JavaConversions._

object PropertyGraphFactory {

  val sparkSession: SparkSession = SparkSession.builder()
    .appName("GraphFactory")
    .master("local")
    .getOrCreate()
  val sQLContext: SQLContext = sparkSession.sqlContext

  def createGraph(resource: Resource): GraphFrame = {
    val dataFrames: (DataFrame, DataFrame) = createVertices(resource)
    GraphFrame(dataFrames._1, dataFrames._2)
  }

  def getResourceID(resource: Resource): String = {
    if (resource.isAnon)
      resource.getId.toString
    else
      resource.getURI

  }


  def createVertices(resource: Resource): (DataFrame, DataFrame) = {

    val propertyList = resource.listProperties().toList
    if (propertyList.isEmpty) {
      return null
    }
    val propertiesAndEdges: (Stream[Statement], Stream[Statement]) = propertyList.toStream
      .partition(stmt => stmt.getObject.canAs(classOf[Literal]))

    val properties: Seq[(String, AnyRef)] = extractProperties(resource, propertiesAndEdges)
    val structFields: Seq[StructField] = properties.map(property => StructField(property._1, inferField(property._2), true))
    val schema = StructType(structFields)

    val edges = propertiesAndEdges._2
      .map(stmt => (resource, stmt.getPredicate.getLocalName, stmt.getObject.asResource()))

    val children: Stream[(DataFrame, DataFrame)] = edges.map(stmt => stmt._3)
      .map(createVertices)
      .filter(obj => obj != null)

    val edgeDataFrames: DataFrame = if (children != null && children.nonEmpty)
      children.map(dataFrames => dataFrames._2)
        .reduce((curr, mem) => curr.union(mem))
        .union(
          sQLContext.createDataFrame(edges.map(stmt => (getResourceID(stmt._1), getResourceID(stmt._3), stmt._2))
            .toList).toDF("src", "dst", "relationship"))
    else
      sQLContext.createDataFrame(edges.map(stmt => (getResourceID(stmt._1), getResourceID(stmt._3), stmt._2))
        .toList).toDF("src", "dst", "relationship")


    val propertyValues: Seq[Any] = properties.map(property => DataTypeHelper.getValue(schema.fields.apply(schema.fieldIndex(property._1)), property._2))
    val vertexDataSet: DataFrame = sQLContext.createDataFrame(List(Row.fromSeq(propertyValues)), schema)

    val vertexDataFrames: DataFrame = if (children != null && children.nonEmpty)
      children.map(dataFrames => dataFrames._1)
        .reduce((curr, mem) => curr.union(mem))
        .union(
          vertexDataSet)
    else
      vertexDataSet

    (vertexDataFrames, edgeDataFrames)

  }


  private def extractProperties(resource: Resource, propertiesAndEdges: (Stream[Statement], Stream[Statement])) = {

    def getPropertyValues(property: Property) = {
      val propertyValues = resource.listProperties(property).toStream.
        flatMap(stmt => Seq(stmt.getObject.asLiteral().getValue))
        .filter(value => value != null)
      if (propertyValues.isEmpty)
        null
      else if (propertyValues.size == 1)
        (property.getLocalName, propertyValues.head)
      else
        (property.getLocalName, propertyValues)

    }

    def getResourceType(): String = {
      val resourceTypes: Seq[RDFNode] = propertiesAndEdges._2
        .filter(stmt => stmt.getPredicate.getLocalName.equals("type"))
        .map(stmt => stmt.getObject)
        .filter(obj => obj.canAs(classOf[Resource]))

      if (resourceTypes.isEmpty) null else resourceTypes.head.asResource().getLocalName

    }

    val properties: Seq[(String, AnyRef)] = List(("id", getResourceID(resource)), ("type", getResourceType()))
      .++(
        propertiesAndEdges._1
          .map(stmt => stmt.getPredicate)
          .distinct
          .map(property =>
            getPropertyValues(property)
          )
      )
      .filter(tup => tup._1 != null || tup._2 != null)
    properties
  }
}
