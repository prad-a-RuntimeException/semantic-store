package recipestore.graph

import javax.inject.{Inject, Named}

import org.graphframes.GraphFrame

import scala.language.postfixOps


/**
  * Resources to GraphFrame create.
  */
class GraphLoader @Inject()(@Named("graphDirectory") val graphDirectory: String) extends Serializable {


  def loadFromFile(): GraphFrame = {

    val context = PropertyGraphFactory.sparkSession.sqlContext

    val vertices = context.read.parquet(s"$graphDirectory/vertices")
    val edges = context.read.parquet(s"$graphDirectory/edges")
    GraphFrame(vertices, edges)
  }

}
