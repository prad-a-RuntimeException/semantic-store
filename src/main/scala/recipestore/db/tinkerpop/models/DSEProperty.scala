package recipestore.db.tinkerpop.models

import recipestore.db.tinkerpop.models.DSEPropertyType.DSEPropertyType

abstract class DSEProperty {
  val label: String
  val isMultiple: Boolean
  val propertyType: DSEPropertyType

  def canEqual(other: Any): Boolean = other.isInstanceOf[DSEProperty]

  override def equals(other: Any): Boolean = other match {
    case that: DSEProperty =>
      (that canEqual this) &&
        label == that.label &&
        propertyType == that.propertyType
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(label, propertyType)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}