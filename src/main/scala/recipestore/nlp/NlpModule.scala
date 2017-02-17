package recipestore.nlp

import com.google.common.base.MoreObjects
import com.google.common.io.Resources
import com.google.inject.{Provides, Singleton}
import net.codingwell.scalaguice.ScalaModule
import org.apache.lucene.analysis.Analyzer
import recipestore.ResourceLoader
import recipestore.graph.GraphModule
import recipestore.misc.OptionConvertors._
import recipestore.nlp.corpus.WordnetApi
import recipestore.nlp.corpus.ingredient.WordnetCorpusFactory
import recipestore.nlp.lucene.LuceneDAO

object NlpModule {
  val wordnetIndexDir = "wordnet"
  val ingredientIndexDir = "ingredient"
}

class NlpModule(indexDir: String, createNewIndex: Boolean = false) extends GraphModule with ScalaModule {

  final lazy val baseDir: String = ResourceLoader.get.apply(ResourceLoader.Resource.lucene, "index-dir").toOption.getOrElse("")

  var analyzer: Analyzer = null
  final val wordnetStream = Resources.getResource("wordnet.fn").openStream()

  def this(indexDir: String, analyzer: Analyzer) = {
    this(indexDir, true)
    this.analyzer = analyzer
  }

  @Provides
  @Singleton
  def getLuceneDAO: LuceneDAO = {
    return new LuceneDAO(s"$baseDir/$indexDir", createNewIndex)
  }

  @Provides
  def getWordnetCorspusFactory: WordnetCorpusFactory = {
    return WordnetCorpusFactory.apply()
  }


  @Provides
  def getWordnetApi: WordnetApi = {
    return new WordnetApi(wordnetStream)
  }

  override protected def configure(): Unit = {
    super.configure()
    bind(classOf[Analyzer])
      .toInstance(MoreObjects.firstNonNull(analyzer, getWordnetCorspusFactory.analyzer))
  }
}
