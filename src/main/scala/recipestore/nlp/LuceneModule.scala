package recipestore.nlp

import com.google.inject.{Provides, Singleton}
import net.codingwell.scalaguice.ScalaModule
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import recipestore.ResourceLoader.Resource.lucene
import recipestore.ResourceLoader.get
import recipestore.db.triplestore.CommonModule
import recipestore.misc.OptionConvertors._

class LuceneModule extends CommonModule with ScalaModule {

  final lazy val indexDirectory: String = get.apply(lucene, "index-dir").toOption.getOrElse("")
  final val analyzer: Analyzer = new StandardAnalyzer()

  @Provides
  @Singleton
  def getLuceneDAO: LuceneDAO = {
    return new LuceneDAO(indexDirectory)
  }

  override protected def configure(): Unit = {
    super.configure()
    bind(classOf[Analyzer])
      .toInstance(analyzer)
  }
}
