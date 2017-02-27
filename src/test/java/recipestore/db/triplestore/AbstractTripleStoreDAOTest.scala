package recipestore.db.triplestore


import org.apache.jena.rdf.model.Resource
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.collection.JavaConverters._

abstract class AbstractTripleStoreDAOTest extends FunSuite with Matchers with BeforeAndAfterAll {
  def getTripleStoreDAO: TripleStoreDAO

  def shouldGetModelAndListStatements() {
    val statements: List[Resource] = getTripleStoreDAO.getResource("http://schema.org/Recipe").asScala.toList
    statements.size > 0 should be(true)
    statements.size should be > 10
  }

  override def afterAll(): Unit = {
    getTripleStoreDAO.delete(true)
  }
}