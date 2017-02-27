package recipestore.graph

import java.util.stream.Stream
import javax.inject.{Inject, Named}

import com.google.inject.Guice
import org.apache.jena.rdf.model.Resource
import org.apache.spark.sql.SaveMode
import org.graphframes.GraphFrame
import org.slf4j.{Logger, LoggerFactory}
import recipestore.input.RecipeResourceFilter.getRecipeWithMinimumNumberOfRating
import recipestore.input.{RecipeApi, RecipeResourceFilter}

import scala.collection.JavaConverters._
import scala.language.postfixOps


/**
  * Resources to GraphFrame create.
  */
object GraphCreator {
  def main(args: Array[String]): Unit = {
    val graphCreator: GraphCreator = Guice.createInjector(new GraphModule).getInstance(classOf[GraphCreator])
    graphCreator.write(-1)
  }
}

class GraphCreator @Inject()(val recipeApi: RecipeApi, @Named("graphDirectory") val graphDirectory: String) {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[GraphCreator])

  def load(limit: Int): GraphFrame = {
    val data: Stream[Resource] = recipeApi.getRecipeData(getRecipeWithMinimumNumberOfRating)
    PropertyGraphFactory.createGraph(data.iterator().asScala.toStream, limit)
  }

  def write(limit: Int): Unit = {
    val graph = load(limit)
    LOGGER.info("Started writing data")
    graph.vertices.write.mode(SaveMode.Overwrite).parquet(s"$graphDirectory/vertices")
    graph.edges.write.mode(SaveMode.Overwrite).parquet(s"$graphDirectory/edges")
  }


}
