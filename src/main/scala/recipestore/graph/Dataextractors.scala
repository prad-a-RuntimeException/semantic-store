package recipestore.graph

import org.apache.spark.sql.{DataFrame, Dataset, Row}

object Dataextractors {

  def getRowsOfType(df: DataFrame, t: String): Dataset[Row] = {
    df.filter(row => getRowVal(row, "type").equals(t))
  }

  def getRowVal(row: Row, field: String): Any = {
    Option(row.get(row.fieldIndex(field))).getOrElse("")
  }


}
