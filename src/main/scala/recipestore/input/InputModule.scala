package recipestore.input

import java.io.{IOException, InputStream}
import java.nio.file.{Files, Paths, StandardOpenOption}

import com.google.inject.Provides
import com.google.inject.name.Names
import recipestore.db.triplestore.{FileBasedTripleStoreDAO, TripleStoreDAO}
import recipestore.{AppResource, CommonModule, ResourceLoader}

object InputModule {
  private val DATASET_FILE_LOC: String = ResourceLoader.apply(AppResource.TriplestoreResource, "input-file").get

  @Provides def providesDatasetStream: InputStream = {
    try {
      return Files.newInputStream(Paths.get(DATASET_FILE_LOC), StandardOpenOption.READ)
    }
    catch {
      case e: IOException => {
        throw new RuntimeException("Failed initializing module. Input quad file is mandatory")
      }
    }
  }
}

class InputModule extends CommonModule {
  override protected def configure() {
    super.configure()
    bind(classOf[TripleStoreDAO]).to(classOf[FileBasedTripleStoreDAO])
    bind(classOf[String]).annotatedWith(Names.named("datasetName")).toInstance("recipe")
  }
}