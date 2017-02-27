package recipestore.db.tinkerpop

import com.twitter.storehaus.cache.{MapCache, MutableCache}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row}
import recipestore.db.tinkerpop.models.DSEPropertyType.DSEPropertyType
import recipestore.db.tinkerpop.models._

import scala.collection.JavaConverters._

object DataFrameToDSEVertex {

  implicit val graphVertexEncoder = org.apache.spark.sql.Encoders.kryo[DSEGraphVertex]

  val vertexCache: MutableCache[String, DSEGraphVertex] =
    MapCache.empty[String, DSEGraphVertex].toMutable()

  def apply(dataFrame: DataFrame, textIndexProperties: List[String] = List(),
            indexProperties: List[String] = List("resourceId")): Iterable[DSEGraphVertex] = {
    dseVertices(dataFrame, textIndexProperties, indexProperties).toIterable
  }

  private def dseVertices(dataFrame: DataFrame, textIndexProperties: List[String] = List(),
                          indexProperties: List[String] = List()): Iterator[DSEGraphVertex] = {

    val materializedIndex: DSEPropertyIndex = new DSEPropertyIndex("materialized", DSEIndexType.Materialized, indexProperties)
    val textIndex: DSEPropertyIndex = new DSEPropertyIndex("search", DSEIndexType.TextSearch, textIndexProperties)

    def dseVertex(row: Row): DSEGraphVertex = {
      val typeVal = if (row.schema.fields.map(f => f.name).find(str => str.equals("type")).isDefined)
        Option(row.getString(row.fieldIndex("type")))
          .getOrElse("")
      else "Default"
      if (!typeVal.isEmpty) {
        if (vertexCache.get(typeVal).isEmpty) {
          val vertex = new DSEGraphVertex(typeVal,
            dseVertexProperties(row.schema, typeVal).toList,
            List(),
            List(textIndex, materializedIndex)) {}
          vertexCache.+=(typeVal, vertex)
        }
        vertexCache.get(typeVal).get
      } else
        null
    }

    def getDataType(dataType: DataType): DSEPropertyType = {
      dataType match {
        case StringType => DSEPropertyType.Text
        case IntegerType => DSEPropertyType.Int
        case DoubleType => DSEPropertyType.Double
        case BooleanType => DSEPropertyType.Boolean
        case _ => DSEPropertyType.Text
      }
    }

    def dseVertexProperties(schema: StructType, typeVal: String): Iterable[DSEProperty] = {
      schema.fields.map(f => new DSEProperty {

        override val propertyType = getDataType(f.dataType)

        override val label = if (f.name.equals("id")) "resourceId" else f.name

        override val isMultiple: Boolean = {
          f.dataType match {
            case _: ArrayType => true
            case _ => false
          }
        }
      })
    }

    dataFrame.map(row => dseVertex(row)).toLocalIterator().asScala

  }

}
