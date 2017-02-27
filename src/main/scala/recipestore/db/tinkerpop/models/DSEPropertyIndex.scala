package recipestore.db.tinkerpop.models

import recipestore.db.tinkerpop.models.DSEIndexType.DSEIndexType


class DSEPropertyIndex(val name: String, val indexType: DSEIndexType, val properties: List[String]) extends Serializable {

  def canEqual(other: Any): Boolean = other.isInstanceOf[DSEPropertyIndex]

  override def equals(other: Any): Boolean = other match {
    case that: DSEPropertyIndex =>
      (that canEqual this) &&
        name == that.name &&
        indexType == that.indexType
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(name, indexType)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}