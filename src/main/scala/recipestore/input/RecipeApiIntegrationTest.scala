package recipestore.input

import java.io.InputStream

import org.apache.jena.ext.com.google.common.io.Resources
import org.apache.jena.rdf.model.Resource
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import recipestore.db.triplestore.FileBasedTripleStoreDAO

object RecipeApiIntegrationTest extends FunSuite with Matchers with BeforeAndAfter {
  private var recipeApi: RecipeApi = null

  def before() {
    val recipeStream: InputStream = Resources.getResource("recipe.nq").openStream
    recipeApi = new RecipeApi(new FileBasedTripleStoreDAO("int-test"), recipeStream)
    recipeApi.loadRecipe(true)
  }

  def shouldGetRecipeDataFromTheTripleStore() {
    val recipeData: Seq[Resource] = recipeApi.getRecipeData(RecipeResourceFilter.getRecipeWithMinimumNumberOfRating)
    recipeData.size should be > 1
  }
}
