package recipestore.nlp

import javax.inject.Inject

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.simple.SimpleQueryParser
import org.apache.lucene.search.{IndexSearcher, TopDocs}

import scala.collection.JavaConverters._

class LuceneSearchApi @Inject()(val luceneDAO: LuceneDAO, val analyzer: Analyzer) {
  lazy val indexReader = DirectoryReader.open(luceneDAO.index)
  lazy val indexSearcher = new IndexSearcher(indexReader)

  /**
    * Simplest query implementation
    *
    * @param queryStr
    * @param field
    * @param numResults
    * @return
    * TODO: A query is made up of InputQueryParam and how to render the output and hence it makes sense for
    * it to be composed of three different kind of functions, the QueryCreator, Executor and Wrapper. Create
    * a fluent interface of all these values
    */
  def query(queryStr: String, field: String, numResults: Int = 10): Iterable[Map[String, Iterable[String]]] = {
    val search: TopDocs = indexSearcher.search(new SimpleQueryParser(analyzer, field).parse(queryStr), numResults)
    LuceneSearchResultWrapper.wrapAsSimpleList(search)
  }


  object LuceneSearchResultWrapper {

    def wrapAsSimpleList(topDocs: TopDocs): Iterable[Map[String, Iterable[String]]] = wrapAsDocument(topDocs)
      .map(document => document.getFields.asScala.map(field => field.name() -> document.getFields(field.name()).map(_.stringValue())
        .toIterable).toMap)
      .toIterable

    private def wrapAsDocument(topDocs: TopDocs) = topDocs.scoreDocs.map(result => indexReader.document(result.doc))
  }

}
