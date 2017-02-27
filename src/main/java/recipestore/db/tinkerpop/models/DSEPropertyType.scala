package recipestore.db.tinkerpop.models

object DSEPropertyType extends Enumeration {
  type DSEPropertyType = Value
  val Text, Int, Uuid, Decimal, Double, Float, Timestamp, Boolean, Duration = Value
}