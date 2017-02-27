package recipestore.db.triplestore.rdfparsers

import org.apache.jena.riot.tokens.{TokenType, Tokenizer}
import org.scalatest.{FunSuite, Matchers}

import scala.collection.JavaConverters._

class LenientTokenizerTest extends FunSuite with Matchers {
  test("Should tokenize bad uri") {
    val expected: List[TokenType] = List(TokenType.BNODE, TokenType.IRI, TokenType.LITERAL_LANG, TokenType.IRI, TokenType.DOT)
    val expectedValue: List[String] = List("node2ea0be87cb5aa410715f477d8126cd8f", "http://schema.org/NutritionInformation/sodiumContent", "Sodium 480mg", "http://relish.com/recipes/creamy-braising-greens-soup/", null)
    val quadWithBadURI: String = "_:node2ea0be87cb5aa410715f477d8126cd8f <http://schema.org/NutritionInformation/\\\"sodiumContent\\\"> \"Sodium 480mg\"@en-us <http://relish.com/recipes/creamy-braising-greens-soup/> ."
    val tokenizer: Tokenizer = LenientTokenizer.create(quadWithBadURI)
    val actualValues: Seq[(TokenType, String)] = tokenizer.asScala.toSeq.map(token => (token.getType(), token.getImage()))
    actualValues.map(v => v._1) should be equals expected
    actualValues.map(v => v._2) should be equals expectedValue
  }
}