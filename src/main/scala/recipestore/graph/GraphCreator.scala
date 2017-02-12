package recipestore.graph

import java.util.stream.Stream
import javax.inject.{Inject, Named}

import org.apache.jena.rdf.model.Resource
import org.apache.spark.sql.SaveMode
import org.graphframes.GraphFrame
import recipestore.input.RecipeApi

import scala.collection.JavaConverters._
import scala.language.postfixOps


/**
  * Resources to GraphFrame create.
  */
class GraphCreator @Inject()(val recipeApi: RecipeApi, @Named("graphDirectory") val graphDirectory: String) {


  def load(limit: Int): GraphFrame = {
    val data: Stream[Resource] = recipeApi.getRecipeData
    PropertyGraphFactory.createGraph(data.iterator().asScala.toStream, limit)
  }

  def write(limit: Int): Unit = {

    val graph = load(limit)
    graph.vertices.write.mode(SaveMode.Overwrite).parquet(s"$graphDirectory/vertices")
    graph.edges.write.mode(SaveMode.Overwrite).parquet(s"$graphDirectory/edges")
  }

  def loadFromFile(): GraphFrame = {

    val context = PropertyGraphFactory.sparkSession.sqlContext

    val vertices = context.read.parquet(s"$graphDirectory/vertices")
    val edges = context.read.parquet(s"$graphDirectory/edges")
    GraphFrame(vertices, edges)
  }

}
