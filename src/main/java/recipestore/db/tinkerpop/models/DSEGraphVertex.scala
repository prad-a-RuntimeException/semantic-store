package recipestore.db.tinkerpop.models


class DSEGraphVertex(val label: String, val properties: List[DSEProperty],
                     val edges: List[DSEGraphEdge],
                     val propertyIndices: List[DSEPropertyIndex]) {
  def getLabel: String = {
    label
  }

  def getProperties: List[DSEProperty] = {
    properties
  }

  def getEdges: List[DSEGraphEdge] = {
    edges
  }

  def getPropertyIndices: List[DSEPropertyIndex] = {
    propertyIndices
  }


  def canEqual(other: Any): Boolean = other.isInstanceOf[DSEGraphVertex]

  override def equals(other: Any): Boolean = other match {
    case that: DSEGraphVertex =>
      (that canEqual this) &&
        label == that.label
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(label)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}