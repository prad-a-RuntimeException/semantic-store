package recipestore.nlp.corpus.ingredient

import java.util

import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.ling.tokensregex.{MultiPatternMatcher, SequenceMatchResult, TokenSequencePattern}
import edu.stanford.nlp.util.CoreMap
import recipestore.nlp.NlpPipeline

import scala.collection.JavaConverters._

object RecipeReviewParser {

  private def substitutionPattern: MultiPatternMatcher[CoreMap] = {
    val pattern1 = "[{lemma:/replace.*/}|{lemma:/substitute.*/}] (?$ing1[ { tag:/NN.*/ } |  { tag:/JJ.*/ } ]*) /with/ (?$ing2[ { tag:/NN.*/ } |  { tag:/JJ.*/ } ]*)"
    val pattern2 = " (?$ing2[ { tag:/NN.*/ } |  { tag:/JJ.*/ } ]*) instead of  (?$ing1[ { tag:/NN.*/ } |  { tag:/JJ.*/ } ]*)"
    val seqPattern1 = TokenSequencePattern.compile(pattern1)
    val seqPattern2 = TokenSequencePattern.compile(pattern2)
    val matcher: MultiPatternMatcher[CoreMap] = TokenSequencePattern.getMultiPatternMatcher(seqPattern1, seqPattern2)
    matcher
  }


  def apply(input: String): Iterable[IngredientSubstitution] = {

    val tokenList: Iterable[CoreLabel] = NlpPipeline(input)
    val multiMatcher: MultiPatternMatcher[CoreMap] = substitutionPattern
    val coreLabels: util.List[CoreLabel] = tokenList.toList.asJava
    val patternMatcher: util.List[SequenceMatchResult[CoreMap]] = multiMatcher
      .findNonOverlapping(coreLabels)
    patternMatcher.asScala.map(matcher => {
      val ing1 = matcher.group("$ing1")
      val ing2 = matcher.group("$ing2")
      if (ing1 != null && ing2 != null)
        new IngredientSubstitution(input, ing1, ing2)
      else null
    }).filter(_ != null)
  }

}
