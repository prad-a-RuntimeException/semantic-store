package recipestore.nlp.corpus.ingredient.stats.models

//Order agnostic tuple
class Pair(val first: String, val second: String) {
  private val values = Set(first, second)

  def canEqual(other: Any): Boolean = other.isInstanceOf[Pair]

  override def equals(other: Any): Boolean = other match {
    case that: Pair =>
      (that canEqual this) &&
        values == that.values
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(values)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
