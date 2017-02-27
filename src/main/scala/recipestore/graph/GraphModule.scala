package recipestore.graph

import java.util.ResourceBundle

import com.google.inject.{Guice, Provides}
import net.codingwell.scalaguice.ScalaModule
import recipestore.input.{InputModule, RecipeApi}

object GraphModule {
  val GRAPHFRAME_DIR = "graphframe_dir"

  def graphDirectory: String = {
    val bundle = ResourceBundle.getBundle("graph")
    assert(bundle.containsKey(GRAPHFRAME_DIR))
    String.format("file://%s", bundle.getString(GRAPHFRAME_DIR))
  }
}

class GraphModule extends InputModule with ScalaModule {

  @Provides
  def getRecipeApi(): RecipeApi = {
    Guice.createInjector(new InputModule()).getInstance(classOf[RecipeApi])
  }

  override protected def configure(): Unit = {
    super.configure()
  }
}
