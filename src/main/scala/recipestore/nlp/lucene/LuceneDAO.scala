package recipestore.nlp.lucene

import java.io.File
import java.nio.file.Paths.get

import org.apache.commons.io.FileUtils
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.store.FSDirectory.open

class LuceneDAO(val indexDir: String, val createNew: Boolean = false) {
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
}
