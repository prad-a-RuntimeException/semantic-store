package recipestore

import com.google.inject.{Guice, Injector}
import recipestore.input.{InputModule, RecipeApi}

object TripleStoreEntryPoint {
  var inputModule: Injector = Guice.createInjector(new InputModule)

  def main(args: Array[String]) {
    loadRecipeData()
  }

  def loadRecipeData() {
    inputModule.getInstance(classOf[RecipeApi]).loadRecipe(true)
  }
}