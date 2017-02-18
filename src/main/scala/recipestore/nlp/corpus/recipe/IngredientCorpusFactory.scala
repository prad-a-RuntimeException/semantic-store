package recipestore.nlp.corpus.recipe

import com.google.inject.Inject
import org.apache.spark.sql.{DataFrame, Dataset, Row}
import org.graphframes.GraphFrame
import recipestore.graph.{Dataextractors, GraphLoader}
import recipestore.nlp.corpus.WordnetApi
import recipestore.nlp.corpus.ingredient.WordnetCorpusFactory

class IngredientCorpusFactory @Inject()(override val wordnetApi: WordnetApi, val graphLoader: GraphLoader)
  extends WordnetCorpusFactory(wordnetApi) {

  private lazy val graph: GraphFrame = graphLoader.loadFromFile()
  private lazy val recipeRows: Dataset[Row] = Dataextractors.getRowsOfType(graph.vertices, "Recipe")
  private lazy val recipeReviews: DataFrame = graph.find("(recipe)-[e]->(review)")
    .filter("e.relationship == 'review'")
    .select("recipe.id", "review.id", "review.reviewBody")
    .toDF("id", "reviewId", "review")

  val sql = recipeReviews.sparkSession.sqlContext.sql _


  lazy val datasets: DataFrame = {

    recipeRows.createOrReplaceTempView("recipe")
    recipeReviews.createOrReplaceTempView("individualRecipeReviews")
    sql("SELECT id,collect_list(review) as reviews  FROM individualRecipeReviews GROUP BY id")
      .createOrReplaceTempView("reviewsCollectedById")
    sql("select * from recipe left outer join reviewsCollectedById on recipe.id = reviewsCollectedById.id")
  }

}
