package recipestore.db.tinkerpop

import com.datastax.driver.dse.DseSession
import com.datastax.driver.dse.graph.GraphResultSet
import org.apache.spark.sql.DataFrame
import org.slf4j.{Logger, LoggerFactory}
import recipestore.db.tinkerpop.models.DSEGraphVertex
import recipestore.graph.SparkFactory

object RecipeCosineSimilarityGraph {

  val graphName: String = "Recipe"
  val sessionFn: (String) => DseSession = DseGraphDAO.dseSession(_)

  def load(sparkFactory: SparkFactory, recipes: () => DataFrame, recipeCosineSimilarity: () => DataFrame): Boolean = {

    val sparkContext = sparkFactory.sparkSession.sparkContext
    val recipeCount = sparkContext.longAccumulator("RecipeCount")

    val logger: Logger = LoggerFactory.getLogger(classOf[RecipeCosineSimilarityGraph])

    val vertex: Iterable[DSEGraphVertex] = DataFrameToDSEVertex(recipes())
    val schemaScripts: List[String] = DseSchemaCreator.createSchemaThroughTraversal(vertex.toList)
    val session: DseSession = sessionFn(graphName)
    DseGraphDAO.clearSchema(session)
    val schemaFutures: Iterable[GraphResultSet] = schemaScripts.map(schemaQuery => {
      logger.info(s"Executing schema script $schemaQuery ")
      DseGraphDAO.execute(session, schemaQuery)
    })


    recipes()
      .limit(1000)
      .repartition(1)
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
      })

    logger.info("Done inserting vertices")

    Thread.sleep(1000 * 60)

    logger.info("Started inserting edges")

    val recipeCosineEdgesCount = sparkContext.longAccumulator("RecipeCosineEdgesCount")

    recipeCosineSimilarity()
      .limit(100)
      .repartition(1)
      .foreachPartition(p => {
        val session = sessionFn(graphName)
        p.foreach(row => {
          recipeCosineEdgesCount.add(1)
          if (recipeCosineEdgesCount.count % 10000 == 0) {
            logger.info("Recipe edge count {} ", recipeCosineEdgesCount.count)
          }
          val propertyMap: Map[String, AnyRef] = row.schema.fields
            .map(f => (f.name -> row.get(row.fieldIndex(f.name))))
            .toMap
            .+("id1" -> row.getString(row.fieldIndex("src")))
            .+("id2" -> row.getString(row.fieldIndex("dst")))
            .+("similarity" -> row.getDouble(row.fieldIndex("relationship")))
            .map(x => (x._1 -> x._2.asInstanceOf[AnyRef]))
          DseGraphDAO.executeAsync(session, GremlinQueryFactory.addEdgeScript("similarity"), propertyMap.filter(k => k._2 != null))
        })
        Thread.sleep(10000)
      })

    true

  }

}

class RecipeCosineSimilarityGraph(
                                   recipeCosineSimilarity: DataFrame, recipes: DataFrame) extends Serializable {

  //  dseGraphDAO.drop()

  def load: Boolean = (() => RecipeCosineSimilarityGraph.load(new SparkFactory {}, () => recipes, () => recipeCosineSimilarity))
    .apply()

}
