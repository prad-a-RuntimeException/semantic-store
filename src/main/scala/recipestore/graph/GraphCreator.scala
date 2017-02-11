package recipestore.graph

import java.util.stream.Stream

import org.apache.jena.rdf.model.Resource
import org.apache.spark.sql.SaveMode
import org.graphframes.GraphFrame
import recipestore.input.DaggerInputComponent

import scala.collection.JavaConverters._
import scala.language.postfixOps


/**
  * Resources to GraphFrame create.
  */
object GraphCreator {

  def main(args: Array[String]): Unit = {
    write(load())
  }

  def load(): GraphFrame = {
    val inputComponent = DaggerInputComponent.builder.build
    val data: Stream[Resource] = inputComponent.getRecipeApi.getRecipeData
    PropertyGraphFactory.createGraph(data.iterator().asScala.toStream, 10)
  }

  def write(graph: GraphFrame): GraphFrame = {
    val graphComponent = DaggerGraphComponent.builder().build().getGraphDirectory.get()

    graph.vertices.write.mode(SaveMode.Overwrite).parquet(s"$graphComponent/vertices")
    graph.edges.write.mode(SaveMode.Overwrite).parquet(s"$graphComponent/edges")
    graph
  }

}
