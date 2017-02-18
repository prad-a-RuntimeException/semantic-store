package recipestore.nlp

import edu.stanford.nlp.ling.{CoreAnnotation, CoreAnnotations}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

@RunWith(classOf[JUnitRunner])
class NlpPipeline$Test extends FunSuite with Matchers {

  test("Should split input text to tokens encapsulating (including ner and pos)") {

    val tokens = NlpPipeline("Way to much water taste for my family! Terrible recipe, avoid at all costs")

    val expectedVal: List[List[String]] = List(List[String]("NN", "TO", "JJ", "NN", "NN", "IN", "PRP$", "NN", ".", "JJ", "NN", ",", "VB", "IN", "DT", "NNS"),
      List[String]("way", "to", "much", "water", "taste", "for", "my", "family", "!", "terrible", "recipe", ",", "avoid", "at", "all", "cost"))

    List[Class[_ <: CoreAnnotation[String]]](classOf[CoreAnnotations.PartOfSpeechAnnotation],
      classOf[CoreAnnotations.LemmaAnnotation])
      .zipWithIndex
      .foreach(i => {
        val actual = tokens.map(t => t.getString(i._1))
        val expected: List[String] = expectedVal(i._2)
        actual should contain theSameElementsAs (expected)
      }
      )


  }

}
