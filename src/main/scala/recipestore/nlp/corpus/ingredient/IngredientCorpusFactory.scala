package recipestore.nlp.corpus.ingredient

import com.google.common.io.Resources
import recipestore.nlp.corpus.WordnetApi

object IngredientCorpusFactory {
  def apply(): Unit = {
    new IngredientCorpusFactory(new WordnetApi(Resources.getResource("wordnet.fn").openStream()))
  }

  def main(args: Array[String]): Unit = {
    IngredientCorpusFactory.apply()
  }
}

class IngredientCorpusFactory(val wordnetApi: WordnetApi) {


}
