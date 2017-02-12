package recipestore.graph

import com.google.inject.AbstractModule
import com.google.inject.Provides
import java.util.ResourceBundle

import net.codingwell.scalaguice.ScalaModule

object GraphModule {
  val GRAPHFRAME_DIR = "graphframe_dir"

  @Provides def graphDirectory: String = {
    val bundle = ResourceBundle.getBundle("graph")
    assert(bundle.containsKey(GRAPHFRAME_DIR))
    String.format("file://%s", bundle.getString(GRAPHFRAME_DIR))
  }
}

class GraphModule extends AbstractModule with ScalaModule {
  protected def configure() {
  }
}
