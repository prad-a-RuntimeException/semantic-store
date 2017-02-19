package recipestore.nlp.lucene

import java.io.File
import java.nio.file.Paths.get

import org.apache.commons.io.FileUtils
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.store.FSDirectory.open

class LuceneDAO(val indexDir: String, val analyzer: Analyzer, val createNew: Boolean = false) {
  val index: FSDirectory = {
    val file = new File(indexDir)
    if (createNew) {
      FileUtils.deleteQuietly(file)
    }
    if (!file.exists()) {
      FileUtils.forceMkdir(file)
    }

    open(get(indexDir))
  }

  def close = index.close()

  def luceneWriteApi: LuceneWriteApi = new LuceneWriteApi(this, analyzer)

  def luceneSearchAPi: LuceneSearchApi = new LuceneSearchApi(this, analyzer)


  def canEqual(other: Any): Boolean = other.isInstanceOf[LuceneDAO]

  override def equals(other: Any): Boolean = other match {
    case that: LuceneDAO =>
      (that canEqual this) &&
        indexDir == that.indexDir &&
        analyzer == that.analyzer
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(indexDir, analyzer)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
