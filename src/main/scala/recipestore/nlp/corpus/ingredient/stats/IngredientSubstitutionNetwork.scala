package recipestore.nlp.corpus.ingredient.stats

import recipestore.metrics.MeterWrapper
import recipestore.metrics.MetricsFactory.{get, remove}
import recipestore.nlp.corpus.ingredient.stats.models.IngredientSubstitution
import recipestore.nlp.corpus.ingredient.{RecipeReviewParser, StatsCollector}

object IngredientSubstitutionNetwork {

  def apply(): Map[IngredientSubstitution, Double] = {

    val meter = get("IngredientSubstitution", classOf[MeterWrapper])
    val numDocs = StatsCollector.numDocs

    val ingredientCountMap: Map[String, Int] = StatsCollector.ingredients.map(term => (term.termtext.utf8ToString(), term.docFreq))
      .toMap

    val ingredientSubstitution: Iterable[(String, Iterable[IngredientSubstitution])] =
      (1 to (numDocs.toInt - 1))
        .map(i => (StatsCollector.getFieldValue(i, "id"), StatsCollector.getFieldValues(i, "reviews")))
        .filter(v => v._2 != null)
        .flatMap(v => {
          v._2.map(review => {
            meter.poke
            (v._1.toString, RecipeReviewParser(review))
          })
        })

    remove("IngredientSubstitution", classOf[MeterWrapper])

    ingredientSubstitution
      .flatMap(ing => ing._2)
      .groupBy(s => s)
      .map(g => (g._1, g._2.size))
      .map(g => {
        if (ingredientCountMap.getOrElse(g._1.ingredient1, 0) > 0) {
          val directionalPmi = Math.log(g._2.toDouble / ingredientCountMap.get(g._1.ingredient1).get.toDouble)
          (g._1, directionalPmi)
        }
        else {
          null
        }
      })
      .filter(_ != null)

  }

}
