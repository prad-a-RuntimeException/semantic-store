package recipestore.db.tinkerpop

import com.google.inject.Guice.createInjector
import com.google.inject.Provides
import net.codingwell.scalaguice.ScalaModule
import org.apache.spark.sql.DataFrame
import recipestore.ResourceLoader._
import recipestore.db.triplestore.CommonModule
import recipestore.graph.SparkFactory
import recipestore.misc.OptionConvertors._
import recipestore.nlp.NlpModuleApi

object TinkerpopGraphModule {
  def main(args: Array[String]): Unit = {
    TinkerpopGraphModule.apply()
  }

  def apply() {
    //    val recipeCosineSimilarityGraph = Guice.createInjector(new TinkerpopGraphModule).getInstance(classOf[RecipeCosineSimilarityGraph])
    //    recipeCosineSimilarityGraph.load
    val recipeGraph = createInjector(new TinkerpopGraphModule).getInstance(classOf[RecipeToUserGraph])
    recipeGraph.load
    //    val ingredientComplementDistanceGraph = Guice.createInjector(new TinkerpopGraphModule).getInstance(classOf[IngredientComplementGraph])
    //    ingredientComplementDistanceGraph.load
  }

}

class TinkerpopGraphModule extends CommonModule with ScalaModule with SparkFactory {

  val clusterName = get.apply(Resource.tinkerpop, "cluster_name").toOption.getOrElse("")
  val isProductionCode = get.apply(Resource.tinkerpop, "is_production_mode")
    .toOption
    .getOrElse("false")
    .toBoolean

  @Provides
  def recipeGraph(): RecipeToUserGraph = {
    val context = sparkSession.sqlContext
    val verticesDF = context.read.parquet(s"$graphDir/vertices")
    val edgesDF = context.read.parquet(s"$graphDir/edges")
    new RecipeToUserGraph(verticesDF, edgesDF)
  }

  @Provides
  def ingredientComplementSimilarity(): IngredientComplementGraph = {
    val context = sparkSession.sqlContext
    val ingredientComplementSimilarityEdgeDF: DataFrame = context.read.parquet(s"$graphDir/${NlpModuleApi.ingredientComplementSimilarityEdges}")
    val codedIngredientVertices = context.read.parquet(s"$graphDir/${NlpModuleApi.codedIngredients}")
    new IngredientComplementGraph(codedIngredientVertices, ingredientComplementSimilarityEdgeDF)
  }

  @Provides
  def recipeCosineSimilarity(): RecipeCosineSimilarityGraph = {
    val context = sparkSession.sqlContext
    val cosineSimilarityEdgesDF: DataFrame = context.read.parquet(s"$graphDir/${NlpModuleApi.cosineSimilarityEdges}")
    val recipeDataFrame = context.read.parquet(s"$graphDir/vertices")
    val recipeVerticesDF = recipeDataFrame.filter(row => Option(row.getString(row.fieldIndex("type"))).getOrElse("").equals("Recipe"))
    new RecipeCosineSimilarityGraph(cosineSimilarityEdgesDF, recipeVerticesDF)
  }

  override def configure(): Unit = {

  }
}
