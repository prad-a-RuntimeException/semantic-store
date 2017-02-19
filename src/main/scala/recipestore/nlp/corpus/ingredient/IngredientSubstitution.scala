package recipestore.nlp.corpus.ingredient

class IngredientSubstitution(val inputText: String, val ingredient1: String,
                             val ingredient2: String) {
  override def toString: String = String.format("%s,%s,%s", inputText, ingredient1, ingredient2)


  def canEqual(other: Any): Boolean = other.isInstanceOf[IngredientSubstitution]

  override def equals(other: Any): Boolean = other match {
    case that: IngredientSubstitution =>
      (that canEqual this) &&
        ingredient1 == that.ingredient1 &&
        ingredient2 == that.ingredient2
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(ingredient1, ingredient2)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}