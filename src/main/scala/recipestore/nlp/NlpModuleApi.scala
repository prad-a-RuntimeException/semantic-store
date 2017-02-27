package recipestore.nlp

import javax.inject.{Inject, Named}

import com.google.inject.Guice.createInjector
import org.slf4j.{Logger, LoggerFactory}
import recipestore.nlp.corpus.ingredient.NlpStatisticsGenerator

/**
  * The Module Level Interface of the NLP module.
  */
object NlpModuleApi {

  val ingredientComplementSimilarityEdges = "ingredientComplementSimilarityEdges"
  val ingredientSubstitutionSimilarityEdges = "ingredientSubstitutionSimilarityEdges"
  val codedIngredients = "codedIngredientVertices"
  val recipeToCodedIngredientEdges = "recipeToCodedIngredientEdges"
  val cosineSimilarityEdges = "cosineDistanceEdges"

  val statsCreator: NlpStatsDataFrameCreator = createInjector(new NlpModule(NlpModule.ingredientIndexDir))
    .getInstance(classOf[NlpStatsDataFrameCreator])

  def apply(): Unit = {
    statsCreator.createNlpCorpus()
  }

}

class NlpStatsDataFrameCreator @Inject()(@Named("graphDirectory") val graphDirectory: String) {
  val LOGGER: Logger = LoggerFactory.getLogger(classOf[NlpStatsDataFrameCreator])

  def createNlpCorpus() = {
    NlpStatisticsGenerator(graphDirectory, NlpModuleApi.ingredientComplementSimilarityEdges,
      NlpModuleApi.ingredientSubstitutionSimilarityEdges,
      NlpModuleApi.codedIngredients,
      NlpModuleApi.recipeToCodedIngredientEdges,
      NlpModuleApi.cosineSimilarityEdges)
  }
}
