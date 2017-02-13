package recipestore.nlp.corpus.ingredient

import com.google.common.io.Resources
import org.scalatest.{FunSuite, Matchers}
import recipestore.nlp.corpus.WordnetApi

import scala.collection.mutable

class WordnetResourceTest extends FunSuite with Matchers {

  test("Should return traversable wordnet resource") {

    val wordnetData: Map[String, WordnetApi#WordnetResource] =
      new WordnetApi(Resources.getResource("wordnet.fn").openStream())
        .getWordnetFoodData


    wordnetData should contain key "onion"

    wordnetData.getOrElse("onion", null).children should contain allOf("Bermuda_onion", "spring_onion", "scallion")

    wordnetData.getOrElse("scallion", null).names should contain allOf("spring_onion", "green_onion")

    val handled: mutable.Set[String] = mutable.Set()

    def printFoodHierarchy(term: String, level: Int): Unit = {
      handled.add(term)
      val levelIndicator: String = (1 to level).map(_ => '-').mkString("")
      println(s"$levelIndicator$term")
      if (wordnetData.contains(term))
        wordnetData.getOrElse(term, null)
          .children
          .filterNot(handled.contains)
          .foreach(child => printFoodHierarchy(child, level + 1))
    }

    printFoodHierarchy("food", 0)

  }


}
