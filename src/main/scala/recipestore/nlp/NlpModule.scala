package recipestore.nlp

import java.io.InputStream

import com.google.common.io.Resources
import com.google.inject.Provides
import com.twitter.storehaus.cache.{MapCache, Memoize}
import net.codingwell.scalaguice.ScalaModule
import org.apache.lucene.analysis.Analyzer
import recipestore.ResourceLoader
import recipestore.graph.GraphModule
import recipestore.misc.OptionConvertors._
import recipestore.nlp.NlpModule.luceneDAO
import recipestore.nlp.corpus.WordnetApi
import recipestore.nlp.corpus.ingredient.WordnetCorpusFactory
import recipestore.nlp.lucene.LuceneDAO

object NlpModule {
  val wordnetIndexDir = "wordnet"
  val ingredientIndexDir = "ingredient"

  def getLuceneDAO(indexDir: String, analyzer: Analyzer): LuceneDAO = {
    return new LuceneDAO(indexDir, analyzer)
  }

  private val luceneDAOCache = MapCache.empty[(String, Analyzer), LuceneDAO].toMutable()
  private val luceneDAO = Memoize(luceneDAOCache)(getLuceneDAO)
}


class NlpModule(indexDir: String) extends GraphModule with ScalaModule {

  final lazy val baseDir: String = ResourceLoader.get.apply(ResourceLoader.Resource.lucene, "index-dir").toOption.getOrElse("")
  var analyzer: Analyzer = null
  final val wordnetStream: InputStream = Resources.getResource("wordnet.fn").openStream()

  def this(indexDir: String, analyzer: Analyzer) = {
    this(indexDir)
    this.analyzer = analyzer
  }

  @Provides
  def getLuceneDAO: LuceneDAO = {
    luceneDAO(s"$baseDir/$indexDir", getWordnetCorpusFactory.analyzer)
  }

  @Provides
  def getWordnetCorpusFactory: WordnetCorpusFactory = {
    return WordnetCorpusFactory.apply()
  }


  @Provides
  def getWordnetApi: WordnetApi = {
    return new WordnetApi(wordnetStream)
  }

  override protected def configure(): Unit = {
    super.configure()
  }
}
