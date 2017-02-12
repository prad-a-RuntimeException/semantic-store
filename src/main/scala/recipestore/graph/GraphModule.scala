package recipestore.graph

import java.util.ResourceBundle

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import net.codingwell.scalaguice.ScalaModule

object GraphModule {
  val GRAPHFRAME_DIR = "graphframe_dir"

  def graphDirectory: String = {
    val bundle = ResourceBundle.getBundle("graph")
    assert(bundle.containsKey(GRAPHFRAME_DIR))
    String.format("file://%s", bundle.getString(GRAPHFRAME_DIR))
  }
}

class GraphModule extends AbstractModule with ScalaModule {
  protected def configure(): Unit = {
    bind(classOf[String]).annotatedWith(Names.named("graphDirectory")).toInstance(GraphModule.graphDirectory)
  }
}
