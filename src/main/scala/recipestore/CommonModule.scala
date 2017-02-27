package recipestore

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import net.codingwell.scalaguice.ScalaModule

class CommonModule extends AbstractModule with ScalaModule {
  private[recipestore] val GRAPHFRAME_DIR: String = "graphframe_dir"
  final protected val graphDir: String = String.format("file://%s", ResourceLoader.apply(AppResource.GraphResource, GRAPHFRAME_DIR).get)

  override protected def configure() {
    bind(classOf[String]).annotatedWith(Names.named("graphDirectory")).toInstance(graphDir)
  }
}