package recipestore.nlp

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

class NlpModule(indexDir: String, analyzer: Analyzer) extends GraphModule with ScalaModule {

  final lazy val baseDir: String = ResourceLoader.get.apply(ResourceLoader.Resource.lucene, "index-dir").toOption.getOrElse("")

  final val wordnetStream = Resources.getResource("wordnet.fn").openStream()


  def this(indexDir: String) {
    this(indexDir, WordnetCorpusFactory.apply().analyzer)
  }

  @Provides
  @Singleton
  def getLuceneDAO: LuceneDAO = {
    return new LuceneDAO(s"$baseDir/$indexDir", true)
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
      .toInstance(analyzer)
  }
}
