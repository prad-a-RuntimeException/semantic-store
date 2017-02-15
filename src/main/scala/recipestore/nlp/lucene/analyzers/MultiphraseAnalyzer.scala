package recipestore.nlp.lucene.analyzers


import com.lucidworks.analysis.AutoPhrasingTokenFilter
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.core.LowerCaseFilter
import org.apache.lucene.analysis.standard.StandardTokenizer
import org.apache.lucene.analysis.synonym.{SynonymFilter, SynonymMap}
import org.apache.lucene.analysis.{Analyzer, CharArraySet, TokenStream}
import org.apache.lucene.util.CharsRef
import scala.collection.JavaConverters._

class MultiPhraseAnalyzerWithSynonyms(val phrases: Iterable[String], val synonyms: Map[String, Set[String]],
                                      emitSingleTokens: Boolean = true) extends Analyzer {

  val default = new StandardTokenizer()
  val rootFilter = new LowerCaseFilter(_)
  val autoPhraseFilter: (TokenStream) => AutoPhrasingTokenFilter = new AutoPhrasingTokenFilter(_,
    new CharArraySet(phrases.asJavaCollection, true), emitSingleTokens, '_')
  lazy val synonymMap = {
    val builder = new SynonymMap.Builder(true)
    if (!synonyms.isEmpty) {
      synonyms.foreach(entry => entry._2.foreach(value =>
        builder.add(new CharsRef(value), new CharsRef(entry._1), false)))
    }
    builder.build()
  }

  def createComponents(fieldName: String): TokenStreamComponents = {
    val synonymFilter: (TokenStream) => TokenStream = if (synonyms.isEmpty) identity(_) else new SynonymFilter(_, synonymMap, true)
    val filter: TokenStream = (rootFilter andThen autoPhraseFilter andThen synonymFilter).apply(default)
    new Analyzer.TokenStreamComponents(default, filter)
  }

}
