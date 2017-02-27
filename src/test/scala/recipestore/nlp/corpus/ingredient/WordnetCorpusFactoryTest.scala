package recipestore.nlp.corpus.ingredient

import com.google.common.io.Resources.getResource

import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}
import recipestore.nlp.corpus.WordnetApi


class WordnetCorpusFactoryTest extends FunSuite with Matchers {

  val wordnetCorpusFactory: WordnetCorpusFactory = new WordnetCorpusFactory(new WordnetApi(getResource("wordnet.fn")
    .openStream()))

  test("Should get phrases with more that one token") {
    wordnetCorpusFactory.phrases.size should be > 3000
    wordnetCorpusFactory.phrases
      .filterNot(p => p.indexOf('_') >= 0)
      .size should be equals (0)
  }

  test("Should get synonyms with more that one synonymy") {
    wordnetCorpusFactory.synonyms.size should be > 500
    val greenOnionSynonyms = wordnetCorpusFactory.synonyms.find(s => Set("green_onion", "spring_onion", "scallion").contains(s._1))
    greenOnionSynonyms.get._2 should contain allOf("green_onion", "spring_onion", "scallion")

  }

}
