package recipestore.nlp.corpus.ingredient.stats

import recipestore.metrics.MeterWrapper
import recipestore.metrics.MetricsFactory.{get, remove}
import recipestore.misc.CrossProduct.Cross
import recipestore.nlp.corpus.ingredient.StatsCollector
import recipestore.nlp.corpus.ingredient.stats.models.IngredientComplementDistance

object IngredientComplementDistance {

  def apply(): Iterable[IngredientComplementDistance] = {

    val meter = get("ComplementScore", classOf[MeterWrapper])
    val numDocs = StatsCollector.numDocs


    val ingredientCount = StatsCollector.ingredients.map(term => (term.termtext.utf8ToString(), term.docFreq)).toMap

    def calculatePmi(t: ((String, String), Int), ingredientCount: Map[String, Int]) = {
      val pOfAAnB: Double = Math.log(t._2.toDouble /
        (ingredientCount(t._1._1).toDouble * ingredientCount(t._1._2).toDouble))
      new IngredientComplementDistance(t._1._1, t._1._2, pOfAAnB)
    }


    val complementDistance = (1 to (numDocs.toInt - 1))
      .map(i => StatsCollector.allowOnlyTopIngredients(StatsCollector.getTermsForField(i, "ingredients")))
      .flatMap(i => i.cross(i).filterNot(f => f._1.contains(f._2) || (f._2.contains(f._1)))
        .map(i => {
          meter.poke
          new models.Pair(i._1, i._2)
        })
        .toSeq.distinct)
      .groupBy(pair => pair)
      .map(pair => Map((pair._1.first, pair._1.second) -> pair._2.size))
      .flatten
      .map(t => calculatePmi(t, ingredientCount))
      .toList.sorted

    remove("ComplementScore", classOf[MeterWrapper])
    complementDistance
  }


}
