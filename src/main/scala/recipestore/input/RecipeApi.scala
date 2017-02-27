package recipestore.input

import java.io.InputStream
import javax.inject.Inject

import org.apache.jena.rdf.model.Resource
import recipestore.db.triplestore.TripleStoreDAO

import scala.collection.JavaConverters._

class RecipeApi @Inject()(val tripleStoreDAO: TripleStoreDAO, val datasetStream: InputStream) {
  def loadRecipe(forceCreate: Boolean) {
    if (forceCreate) tripleStoreDAO.delete(true)
    tripleStoreDAO.populate(datasetStream)
  }

  def getRecipeData: Iterator[Resource] = {
    return tripleStoreDAO.getResource("http://schema.org/Recipe").asScala
  }

  def getRecipeData(resourceFilter: (Resource) => Boolean): Seq[Resource] = {
    return getRecipeData.toSeq.filter(resourceFilter(_))
  }
}