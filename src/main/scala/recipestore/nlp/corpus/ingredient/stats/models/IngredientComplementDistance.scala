package recipestore.nlp.corpus.ingredient.stats.models

class IngredientComplementDistance(val ing1: String, val ing2: String, val distance: Double) extends Ordered[IngredientComplementDistance] {

  override def compare(that: IngredientComplementDistance): Int = this.distance.compare(that.distance)

  override def toString = s"IngredientComplementDistance($ing1, $ing2, $distance)"
}
