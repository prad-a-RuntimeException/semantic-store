package recipestore.nlp.lucene

import lombok.SneakyThrows
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.{Document, Field, FieldType}
import org.apache.lucene.index.{IndexOptions, IndexWriter, IndexWriterConfig}
import org.slf4j.{Logger, LoggerFactory}

protected class LuceneWriter(luceneDAO: LuceneDAO, val analyzer: Analyzer) {
  val logger: Logger = LoggerFactory.getLogger(classOf[LuceneWriter])
  val conf = new IndexWriterConfig(analyzer)
  conf.setRAMBufferSizeMB(1024)
  val indexWriter = try {
    new IndexWriter(luceneDAO.index, conf)
  }
  catch {
    //Assuming the index is closed, we try one more time before giving up
    case _: Throwable => new IndexWriter(luceneDAO.index, conf)
  }

  /**
    * We only handle String or Iterable of String right now, which
    * may not be sufficient in the future.
    *
    * @param map
    * @return
    */
  def write(map: Map[String, Any]) = {
    val doc: Document = new Document
    val fieldType = getFieldType()
    map.foreach(entry => entry._2 match {
      case _: String =>
        doc.add(new Field(entry._1, entry._2.asInstanceOf[String], fieldType))
      case _: Iterable[_] => {
        entry._2.asInstanceOf[Iterable[_]]
          .filter(e => e != null)
          .foreach(e => {
            doc.add(new Field(entry._1, e.toString, fieldType))
          })
      }
      case _ =>
    })
    indexWriter.addDocument(doc)
  }

  def write(maps: Iterable[Map[String, Any]]): Unit = {
    maps.foreach(write)
    commit()
  }

  def commit() = {
    indexWriter.commit()
  }


  @SneakyThrows
  def getFieldType() = {
    val fieldType = new FieldType
    fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS)
    fieldType.setStored(true)
    fieldType.setStoreTermVectors(true)
    fieldType
  }
}
