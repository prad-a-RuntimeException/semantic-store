package recipestore.nlp.corpus.ingredient.stats.models

class IngredientSubstitutionSimilarity(val ing1: Int, val ing2: Int, val distance: Double) extends Ordered[IngredientComplementSimilarity] {

  override def compare(that: IngredientComplementSimilarity): Int = that.distance.compare(this.distance)

  override def toString = s"IngredientSubstitutionDistance($ing1, $ing2, $distance)"
}
