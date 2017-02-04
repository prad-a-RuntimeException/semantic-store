package recipestore.graph

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date

import org.apache.spark.sql.catalyst.util.DateTimeUtils
import org.apache.spark.sql.types._

import scala.util.control.Exception.allCatch

/**
  * Implmentation based on Spark CSVInferschema function which is private.
  * Used to infer the schemaType dynamically from the object.
  */
object DataTypeHelper {


  def getTimeStamp(dateString: String): Date = {
    val formatStrings = Array("M/y", "M/d/y", "M-d-y")

    val parsedDate: Array[Date] = formatStrings.map(format => {
      try {
        return new SimpleDateFormat(format).parse(dateString)
      } catch {
        case _: Throwable => return null
      }
    })
    parsedDate.find(date => date != null).orNull
  }

  def getValue(structField: StructField, anyRef: AnyRef): Any = {
    if (anyRef == null) {
      anyRef
    }
    else {
      structField.dataType match {
        case _: StringType => anyRef.toString
        case _: IntegerType => anyRef.toString.toInt
        case _: LongType => anyRef.toString.toLong
        case _: DecimalType => anyRef.toString.toDouble
        case _: DoubleType => anyRef.toString.toDouble
        case _: TimestampType => getTimeStamp(anyRef.toString)
        case _: BooleanType => anyRef.toString.toBoolean
        case _: ArrayType => anyRef
        case other: DataType =>
          throw new UnsupportedOperationException(s"Unexpected data type $other")
      }
    }
  }


  def inferField(field: AnyRef, typeSoFar: DataType = NullType): DataType = {
    val strValue = field.toString
    if (field == null) {
      typeSoFar
    } else {
      typeSoFar match {
        case NullType => tryParseCollection(field)
        case IntegerType => tryParseInteger(strValue)
        case LongType => tryParseLong(strValue)
        case _: DecimalType => tryParseDecimal(strValue)
        case DoubleType => tryParseDouble(strValue)
        case TimestampType => tryParseTimestamp(strValue)
        case BooleanType => tryParseBoolean(strValue)
        case StringType => StringType
        case other: DataType =>
          throw new UnsupportedOperationException(s"Unexpected data type $other")
      }
    }
  }

  def mergeRowTypes(first: Array[DataType], second: Array[DataType]): Array[DataType] = {
    first.zipAll(second, NullType, NullType).map { case (a, b) =>
      findTightestCommonType(a, b).getOrElse(NullType)
    }
  }

  private val numericPrecedence: IndexedSeq[DataType] = IndexedSeq(
    ByteType,
    ShortType,
    IntegerType,
    LongType,
    FloatType,
    DoubleType)


  //TODO: this method is not complete.
  val findTightestCommonType: (DataType, DataType) => Option[DataType] = {
    case (t1, t2) if t1 == t2 => Some(t1)
    case (NullType, t1) => Some(t1)
    case (t1, NullType) => Some(t1)
    case (StringType, t2) => Some(StringType)
    case (t1, StringType) => Some(StringType)

    // Promote numeric types to the highest of the two and all numeric types to unlimited decimal
    case (t1, t2) if Seq(t1, t2).forall(numericPrecedence.contains) =>
      val index = numericPrecedence.lastIndexWhere(t => t == t1 || t == t2)
      Some(numericPrecedence(index))

    // These two cases below deal with when `IntegralType` is larger than `DecimalType`.

    // Double support larger range than fixed decimal, DecimalType.Maximum should be enough
    // in most case, also have better precision.
    case (DoubleType, _: DecimalType) | (_: DecimalType, DoubleType) =>
      Some(DoubleType)

    case (t1: DecimalType, t2: DecimalType) =>
      val scale = math.max(t1.scale, t2.scale)
      val range = math.max(t1.precision - t1.scale, t2.precision - t2.scale)
      if (range + scale > 38) {
        // DecimalType can't support precision > 38
        Some(DoubleType)
      } else {
        Some(DecimalType(range + scale, scale))
      }

    case _ => None
  }

  private def tryParseCollection(field: AnyRef): DataType = {
    if (field.isInstanceOf[scala.collection.Iterable[String]]) {
      DataTypes.createArrayType(StringType)
    } else {
      tryParseInteger(field.toString)
    }
  }


  private def tryParseInteger(field: String): DataType = {
    if ((allCatch opt field.toInt).isDefined) {
      IntegerType
    } else {
      tryParseLong(field)
    }
  }

  private def tryParseLong(field: String): DataType = {
    if ((allCatch opt field.toLong).isDefined) {
      LongType
    } else {
      tryParseDecimal(field)
    }
  }

  private def tryParseDecimal(field: String): DataType = {
    val decimalTry = allCatch opt {
      // `BigDecimal` conversion can fail when the `field` is not a form of number.
      val bigDecimal = new BigDecimal(field)
      // Because many other formats do not support decimal, it reduces the cases for
      // decimals by disallowing values having scale (eg. `1.1`).
      if (bigDecimal.scale <= 0) {
        // `DecimalType` conversion can fail when
        //   1. The precision is bigger than 38.
        //   2. scale is bigger than precision.
        DecimalType(bigDecimal.precision, bigDecimal.scale)
      } else {
        tryParseDouble(field)
      }
    }
    decimalTry.getOrElse(tryParseDouble(field))
  }

  private def tryParseDouble(field: String): DataType = {
    if ((allCatch opt field.toDouble).isDefined) {
      DoubleType
    } else {
      tryParseTimestamp(field)
    }
  }

  private def tryParseTimestamp(field: String): DataType = {
    // We keep this for backwords competibility.
    if ((allCatch opt DateTimeUtils.stringToTime(field)).isDefined) {
      TimestampType
    } else {
      tryParseBoolean(field)
    }
  }

  private def tryParseBoolean(field: String): DataType = {
    if ((allCatch opt field.toBoolean).isDefined) {
      BooleanType
    } else {
      stringType()
    }
  }

  // Defining a function to return the StringType constant is necessary in order to work around
  // a Scala compiler issue which leads to runtime incompatibilities with certain Spark versions;
  // see issue #128 for more details.
  private def stringType(): DataType = {
    StringType
  }

}
