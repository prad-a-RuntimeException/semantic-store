package recipestore

import java.nio.file.Paths.get
import java.nio.file.{Files, Path}

import com.google.common.io.Resources.getResource
import org.scalatest.{FunSuite, Matchers}

import scala.collection.JavaConverters._

class ResourceLoaderTest extends FunSuite with Matchers {
  test("Should account for all resources in the path") {
    val resourceFolder: Path = get(getResource("triplestore.properties").getPath).getParent
    val resourceNames: List[String] = Files.list(resourceFolder)
      .iterator().asScala.toList
      .filter(p => p.getFileName.endsWith("properties"))
      .map(p => p.getFileName.toString)
    val actual = AppResource.values
      .map(_.toString)
      .map(name => name + ".properties")
      .toList
    actual should be equals (resourceNames)
  }

  test("Should get a valid resource") {
    val useEmbedded: Option[String] = ResourceLoader(AppResource.TriplestoreResource, "useEmbedded")
    useEmbedded.isDefined should be equals (true)
  }

  test("Shoudl get null value for invalid resource") {
    val useEmbedded: Option[String] = ResourceLoader(AppResource.TriplestoreResource, "somethineElse")
    useEmbedded.isDefined should be equals (false)
  }
}