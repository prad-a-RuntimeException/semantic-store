package recipestore.nlp.corpus.ingredient.stats

import org.apache.spark.sql.Row
import recipestore.metrics.MeterWrapper
import recipestore.metrics.MetricsFactory.{get, remove}
import recipestore.misc.CrossProduct.Cross
import recipestore.nlp.corpus.ingredient.NlpStatisticsGenerator
import recipestore.nlp.corpus.ingredient.NlpStatisticsGenerator.{allowOnlyTopIngredients, getTermsForField, ingredients}
import recipestore.nlp.corpus.ingredient.stats.models.IngredientComplementSimilarity


object IngredientComplementNetwork {

  def apply(): Iterable[IngredientComplementSimilarity] = {

    val meter = get("ComplementScore", classOf[MeterWrapper])
    val numDocs = NlpStatisticsGenerator.numDocs


    val ingredientProbability = ingredients.map(term => (term.termtext.utf8ToString(), term.docFreq / NlpStatisticsGenerator.numDocs)).toMap

    def calculatePmi(t: ((String, String), Int)) = {
      val pOfAAnB: Double = Math.log((t._2.toDouble / NlpStatisticsGenerator.numDocs) /
        (ingredientProbability(t._1._1) * ingredientProbability(t._1._2)))
      new IngredientComplementSimilarity(NlpStatisticsGenerator.indexedIngredient(t._1._1),
        NlpStatisticsGenerator.indexedIngredient(t._1._2), pOfAAnB)
    }

    val complementDistance = (1 to (numDocs.toInt - 1))
      .par
      .map(i => allowOnlyTopIngredients(getTermsForField(i, "ingredients")))
      .flatMap(i => i.cross(i).filterNot(f => f._1.contains(f._2) || (f._2.contains(f._1)))
        .map(i => {
          meter.poke
          new models.Pair(i._1, i._2)
        })
        .toSeq.distinct)
      .groupBy(pair => pair)
      .map(pair => (pair._1.first, pair._1.second) -> pair._2.size)
      .map(t => calculatePmi(t))
      .toList.sorted

    remove("ComplementScore", classOf[MeterWrapper])
    complementDistance
  }


}
