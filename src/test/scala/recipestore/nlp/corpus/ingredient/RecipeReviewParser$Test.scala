package recipestore.nlp.corpus.ingredient

import org.scalatest.FunSuite

class RecipeReviewParser$Test extends FunSuite {

  test("Should extract substitution pattern from the review text") {
    val inputStrings = Array(
      "Replaced  Tomato with Onion",
      "substituted Vinegar with Tomato",
      "used Garlic instead of Onion",
      "olive oil instead of coconut oil")

    inputStrings.map(RecipeReviewParser.apply(_))
      .flatten
      .foreach(println(_))


  }

}
