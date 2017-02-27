package recipestore.db.tinkerpop.models

import java.util.function.Supplier

trait DSEGraphEdge {
  def getLabel: String

  def isMultiple: Boolean

  def getInVertex: ()=>DSEGraphVertex

  def getOutVertex: ()=>DSEGraphVertex

  def getProperties: List[DSEEdgeProperty]
}