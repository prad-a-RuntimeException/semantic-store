package recipestore.nlp.lucene

import com.github.javafaker.Faker
import com.google.inject.{Guice, Injector}
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import recipestore.graph.IntegrationTest
import recipestore.nlp.NlpModule

import scala.collection.immutable.IndexedSeq


class LuceneDAOIntegrationTest extends FunSuite with Matchers with BeforeAndAfter {


  var luceneDAO: LuceneDAO = _
  var searchApi: LuceneSearchApi = _
  var writeApi: LuceneWriter = _

  val faker: Faker = new Faker()

  case class TestDoc(val name: String, val books: List[String]) {

  }

  val testDoc: IndexedSeq[TestDoc] = 1 to 10 map (_ => new TestDoc(faker.name().name(),
    (1 to 5 map (_ => faker.book().title())).toList))

  private val testVals: IndexedSeq[Map[String, AnyRef]] = testDoc.map(doc => Map("name" -> doc.name, "books" -> doc.books))
  assert(testVals.size == 10)

  before {
    val luceneModule: Injector = Guice.createInjector(new NlpModule("test", new StandardAnalyzer()))
    luceneDAO = luceneModule.getInstance(classOf[LuceneDAO])
    searchApi = luceneDAO.luceneSearchAPi
    writeApi = luceneDAO.luceneWriteApi
    writeApi.write(testVals)
  }

  test("Should query the created lucene database", IntegrationTest) {
    val queryResult: Iterable[Map[String, Iterable[String]]] = searchApi.query(testVals.head.get("name").toString, "name", 3)
    // We are using faker and there is statistically minuscule chance of two names having the same token
    queryResult.size should be >= 1
    queryResult.head.get("name").get.head should equal(testVals.head.get("name").get)
    val books = queryResult.head.get("books")
    books.get.size should equal(5)

  }


}
