package recipestore.nlp.lucene.analyzers

import java.io.StringReader

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class MultiPhraseAnalyzerWithSynonymsTest extends FunSuite with Matchers {
  test("Should only match fullterms") {
    val synonymFiles = Map[String, Set[String]]()
    val phraseSets = List("new york", "new york city", "city of new york", "cow milk")
    val phraseAnalyzer = new MultiPhraseAnalyzerWithSynonyms(phraseSets, synonymFiles, false)
    val tokenStream = phraseAnalyzer.tokenStream("myfield", new StringReader(
      """Wow! a new year in New York and I am drinking
      "whole cow milk"""))
    val charTermAttribute = tokenStream.addAttribute(classOf[CharTermAttribute])
    tokenStream.reset()
    val tokens: mutable.Set[String] = mutable.Set()
    while (tokenStream.incrementToken) {
      val term = charTermAttribute.toString
      tokens.add(term)
    }

    tokens should contain allOf("new",
      "year",
      "wow",
      "whole",
      "cow_milk",
      "new_york")
  }


  test("Should use synonyms") {
    //blasphemy
    val synonymFiles = Map[String, Set[String]]("new_new_york" -> Set("new_york", "new_jersey"))
    val phraseSets = List("new york", "new york city", "city of new york", "cow milk")
    val phraseAnalyzer = new MultiPhraseAnalyzerWithSynonyms(phraseSets, synonymFiles, false)
    val tokenStream = phraseAnalyzer.tokenStream("myfield", new StringReader("Wow! a new year in New York"))
    val charTermAttribute = tokenStream.addAttribute(classOf[CharTermAttribute])
    tokenStream.reset()
    val tokens: mutable.Set[String] = mutable.Set()
    while (tokenStream.incrementToken) {
      val term = charTermAttribute.toString
      tokens.add(term)
    }

    tokens should contain allOf("new",
      "year",
      "wow",
      "new_new_york")
  }

}
