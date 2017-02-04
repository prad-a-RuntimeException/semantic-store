package recipestore.graph

import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import org.apache.spark.sql.types._
import org.scalatest.{FunSuite, Matchers}
import org.slf4j.{Logger, LoggerFactory}

class DataTypeHelper$Test extends FunSuite with Matchers {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[DataTypeHelper$Test])

  test("Should parse date object from String") {

    Array("9/1/2009", "9/2009", "9-1-2009", "EEE, dd MMM yyyy HH:mm:ss", "EEE MMM dd HH:mm:ss z yyyy")
      .foreach(format => {
        val date: Date = DataTypeHelper.getTimeStamp(format)
        date should be
        new Date(2009, 9, 1)
      })

  }

  test("Should convert string based on StructType") {

    val expected: List[Any] = List("one", 2, true, 13.0)
    val count: AtomicInteger = new AtomicInteger(0)
    List(StringType, IntegerType, BooleanType, DoubleType)
      .zip(List("one", "2", "true", "13.0"))
      .foreach(dataType => {
        val parsedVal: Any = DataTypeHelper.getValue(StructField.apply("test", dataType._1, true), dataType._2)
        LOGGER.info("Parsed value for {} is {} ", dataType._2, parsedVal)
        assert(parsedVal !== null)
        val apply = expected.apply(count.getAndIncrement())
        parsedVal.should(be(apply))
      })

  }


  test("Should infer the right data type") {

    List("name", 12, true, 14.0)
      .zip(List(StringType, IntegerType, BooleanType, DoubleType))
      .foreach(dataType => {
        val field: DataType = DataTypeHelper.inferField(dataType._1.asInstanceOf[AnyRef])
        LOGGER.info("Date type {}", dataType)
        field.should(be(dataType._2))
      })

  }


}
