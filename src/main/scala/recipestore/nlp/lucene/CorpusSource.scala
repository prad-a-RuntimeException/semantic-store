package recipestore.nlp.lucene

import recipestore.nlp.lucene.analyzers.MultiPhraseAnalyzerWithSynonyms

import scala.collection.immutable.Iterable

trait CorpusSource {

  val phrases: Iterable[String]

  val document: Iterable[Map[String, AnyRef]]

  val synonyms: Map[String, Set[String]]

  val analyzer = new MultiPhraseAnalyzerWithSynonyms(phrases, synonyms, true)

}
