package recipestore.db.tinkerpop

import com.datastax.driver.dse.DseSession
import com.datastax.driver.dse.graph.GraphResultSet
import org.apache.spark.sql.DataFrame
import org.slf4j.{Logger, LoggerFactory}
import recipestore.db.tinkerpop.models.DSEGraphVertex
import recipestore.graph.SparkFactory

object RecipeToUserGraph {

  val graphName: String = "RecipeToUserGraph"
  val sessionFn: (String) => DseSession = DseGraphDAO.dseSession(_)

  def load(sparkFactory: SparkFactory, recipes: () => DataFrame, recipeEdges: () => DataFrame): Boolean = {

    val sparkContext = sparkFactory.sparkSession.sparkContext
    val recipeCount = sparkContext.longAccumulator("RecipeCount")

    val logger: Logger = LoggerFactory.getLogger(classOf[RecipeToUserGraph])

    val vertex: Iterable[DSEGraphVertex] = DataFrameToDSEVertex(recipes())
    val schemaScripts: List[String] = DseSchemaCreator.createSchemaThroughTraversal(vertex.toList)
    val session: DseSession = sessionFn(graphName)
    DseGraphDAO.clearSchema(session)
    val schemaFutures: Iterable[GraphResultSet] = schemaScripts.map(schemaQuery => {
      logger.info(s"Executing schema script $schemaQuery ")
      DseGraphDAO.execute(session, schemaQuery)
    })

    //We are only concerned about Recipe-> AggregateRating -> Review-> ReviewRating vertices
    val allowedVertices: Set[String] = Set("Review", "AggregateRating", "Recipe", "ReviewRating")
    recipes()
      .filter(row => row.schema.fields.find(f => f.name.equals("type")).isDefined)
      .filter(row => allowedVertices.contains(Option(row.getString(row.fieldIndex("type"))).getOrElse(null)))
      .repartition(10)
      .foreachPartition(p => {
        val session = sessionFn(graphName)
        p.foreach(row => {
          recipeCount.add(1)
          if (recipeCount.count % 10000 == 0) {
            logger.info("Recipe count {} ", recipeCount.count)
          }
          val propertyMap: Map[String, AnyRef] = row.schema.fields
            .map(f => (f.name -> row.get(row.fieldIndex(f.name))))
            .map(x => (x._1, x._2.asInstanceOf[Object]))
            .toMap
          DseGraphDAO.executeAsync(session, GremlinQueryFactory.addVertexScript("Recipe", propertyMap), propertyMap
            .filter(k => k._2 != null).map(k => {
            if (k._1.equals("id")) {
              ("resourceId" -> k._2)
            } else (k._1, k._2)
          }))
        })
        Thread.sleep(1000 * 5)
      })

    Thread.sleep(1000 * 60)


    val allowedEdges: Set[String] = Set("review", "aggregateRating", "reviewRating")
    val recipeEdgeCount = sparkContext.longAccumulator("RecipeEdgeCount")
    recipeEdges()
      .filter(row => allowedEdges.contains(Option(row.getString(row.fieldIndex("relationship"))).getOrElse("")))
      .repartition(10)
      .foreachPartition(p => {
        val session = sessionFn(graphName)
        p.foreach(row => {
          recipeEdgeCount.add(1)
          if (recipeEdgeCount.count % 10000 == 0) {
            logger.info("Recipe edge count {} ", recipeEdgeCount.count)
          }
          val propertyMap: Map[String, AnyRef] = row.schema.fields
            .map(f => (f.name -> row.get(row.fieldIndex(f.name))))
            .toMap
            .+("id1" -> row.getString(row.fieldIndex("src")))
            .+("id2" -> row.getString(row.fieldIndex("dst")))
            .map(x => (x._1 -> x._2.asInstanceOf[AnyRef]))
          DseGraphDAO.executeAsync(session, GremlinQueryFactory.addEdgeScript("relationship"),
            propertyMap.filter(k => k._2 != null))
        })
        Thread.sleep(10000)
      })

    true

  }

}

class RecipeToUserGraph(recipes: DataFrame, edges: DataFrame) extends Serializable {
  def load: Boolean = (() => RecipeToUserGraph.load(new SparkFactory {}, () => recipes, () => edges))
    .apply()

}
