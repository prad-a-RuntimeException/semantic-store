package recipestore.db.tinkerpop.models

object DSEIndexType extends Enumeration {
  type DSEIndexType = Value
  val Secondary, Materialized, TextSearch, StringSearch = Value
}