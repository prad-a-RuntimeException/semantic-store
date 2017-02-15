package recipestore.nlp.corpus

class WordnetModel(val names: Set[String], val children: Iterable[String]) {

  override def toString = s"WordnetModel($names)"


  def canEqual(other: Any): Boolean = other.isInstanceOf[WordnetModel]

  override def equals(other: Any): Boolean = other match {
    case that: WordnetModel =>
      (that canEqual this) &&
        names == that.names
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(names)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
