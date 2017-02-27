package recipestore

import java.util.ResourceBundle

object AppResource extends Enumeration {
  type ResourceName = Value
  val TinkerpopResource = Value("tinkerpop")
  val TriplestoreResource = Value("triplestore")
  val GraphResource = Value("graph")
  val LuceneResource = Value("lucene")
}

object ResourceLoader {
  private def resourceBundle(key: String) = ResourceBundle.getBundle(key)

  val resourceCache: Map[AppResource.Value, ResourceBundle] =
    Map(
      AppResource.TinkerpopResource -> resourceBundle(AppResource.TinkerpopResource.toString),
      AppResource.TriplestoreResource -> resourceBundle(AppResource.TriplestoreResource.toString),
      AppResource.GraphResource -> resourceBundle(AppResource.GraphResource.toString),
      AppResource.LuceneResource -> resourceBundle(AppResource.LuceneResource.toString)
    )


  def apply(resource: AppResource.Value, key: String): Option[String] = {
    val resourceBundle = resourceCache.getOrElse(resource, null)
    if (resourceBundle != null && resourceBundle.containsKey(key)) Option(resourceBundle.getString(key))
    else Option.empty
  }


}