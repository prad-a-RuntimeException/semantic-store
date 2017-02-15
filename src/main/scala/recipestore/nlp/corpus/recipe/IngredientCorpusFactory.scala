package recipestore.nlp.corpus.recipe

import com.google.inject.Inject
import org.apache.spark.sql.{DataFrame, Dataset, Row}
import recipestore.graph.{Dataextractors, GraphLoader}
import recipestore.nlp.corpus.WordnetApi
import recipestore.nlp.corpus.ingredient.WordnetCorpusFactory


class IngredientCorpusFactory @Inject()(override val wordnetApi: WordnetApi, val graphLoader: GraphLoader)
  extends WordnetCorpusFactory(wordnetApi) {

  private lazy val recipeRows: Dataset[Row] = Dataextractors.getRowsOfType(graphLoader.loadFromFile().vertices, "Recipe")

  lazy val datasets: DataFrame = recipeRows

}
