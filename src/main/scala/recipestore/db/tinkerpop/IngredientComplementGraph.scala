package recipestore.db.tinkerpop

import com.datastax.driver.dse.DseSession
import com.datastax.driver.dse.graph.GraphResultSet
import org.apache.spark.sql.DataFrame
import org.slf4j.{Logger, LoggerFactory}
import recipestore.db.tinkerpop.models.DSEGraphVertex
import recipestore.graph.SparkFactory

object IngredientComplementGraph {

  val graphName: String = "IngredientComplementGraph"
  val sessionFn: (String) => DseSession = DseGraphDAO.dseSession(_)

  def load(sparkFactory: SparkFactory, ingredientVertices: () => DataFrame, ingredientComplementScoreEdges: () => DataFrame): Boolean = {

    val sparkContext = sparkFactory.sparkSession.sparkContext
    val ingredientCount = sparkContext.longAccumulator("IngredientCount")

    val logger: Logger = LoggerFactory.getLogger(classOf[RecipeCosineSimilarityGraph])

    val vertex: Iterable[DSEGraphVertex] = DataFrameToDSEVertex(ingredientVertices())
    val schemaScripts: List[String] = DseSchemaCreator.createSchemaThroughTraversal(vertex.toList)
    val session: DseSession = sessionFn(graphName)
    DseGraphDAO.clearSchema(session)
    val schemaFutures: Iterable[GraphResultSet] = schemaScripts.map(schemaQuery => {
      logger.info(s"Executing schema script $schemaQuery ")
      DseGraphDAO.execute(session, schemaQuery)
    })


    ingredientVertices()
      .repartition(10)
      .foreachPartition(p => {
        val session = sessionFn(graphName)
        p.foreach(row => {
          ingredientCount.add(1)
          if (ingredientCount.count % 10000 == 0) {
            logger.info("Ingredient count {} ", ingredientCount.count)
          }
          val propertyMap: Map[String, AnyRef] = row.schema.fields
            .map(f => (f.name -> row.get(row.fieldIndex(f.name))))
            .map(x => (x._1, x._2.asInstanceOf[Object]))
            .filter(k => k._2 != null)
            .map(k => {
              if (k._1.equals("id")) {
                ("resourceId" -> k._2)
              } else (k._1, k._2)
            })
            .toMap
          DseGraphDAO.executeAsync(session, GremlinQueryFactory.addVertexScript("Ingredient", propertyMap), propertyMap)
        })
        Thread.sleep(1000 * 5)
      })

    Thread.sleep(1000 * 60)


    val ingredientComplementSimilarityCount = sparkContext.longAccumulator("ComplementSimilarityCount")

    ingredientComplementScoreEdges()
      .repartition(10)
      .foreachPartition(p => {
        val session = sessionFn(graphName)
        p.foreach(row => {
          ingredientComplementSimilarityCount.add(1)
          if (ingredientComplementSimilarityCount.count % 10000 == 0) {
            logger.info("Recipe edge count {} ", ingredientComplementSimilarityCount.count)
          }
          val propertyMap: Map[String, AnyRef] = Map(
            ("id1" -> row.get(row.fieldIndex("src"))),
            ("id2" -> row.get(row.fieldIndex("dst"))),
            ("score" -> row.get(row.fieldIndex("relationship"))))
            .map(x => (x._1 -> x._2.asInstanceOf[AnyRef]))
          DseGraphDAO.executeAsync(session, GremlinQueryFactory.addEdgeScript("similarity", propertyMap), propertyMap)
        })
        Thread.sleep(10000)
      })

    true

  }

}

class IngredientComplementGraph(ingredients: DataFrame, complementSimilarityEdges: DataFrame) extends Serializable {
  def load: Boolean = (() => IngredientComplementGraph.load(new SparkFactory {}, () => ingredients, () => complementSimilarityEdges))
    .apply()

}
