package recipestore.graph

import org.apache.spark.sql.{SQLContext, SparkSession}

trait SparkFactory {

  lazy val sparkSession: SparkSession = SparkSession.builder()
    .appName("GraphFactory")
    .master("local")
    .config("spark.driver.memory", "1g")
    .config("spark.executor.memory", "2g")
    .config("SPARK_CONF_DIR", "./infrastructure/spark-config")
    .getOrCreate()
  lazy val sqlContext: SQLContext = sparkSession.sqlContext

}
