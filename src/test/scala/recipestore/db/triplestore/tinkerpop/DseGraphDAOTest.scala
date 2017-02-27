package recipestore.db.triplestore.tinkerpop

import org.scalatest.{FunSuite, Matchers}
import recipestore.db.tinkerpop.DseGraphDAO
import recipestore.graph.IntegrationTest

class DseGraphDAOTest extends FunSuite with Matchers {

  test("Should be able to open connection to local dse",IntegrationTest) {
    val graphDAO = new DseGraphDAO("local", "Recipe", false)

    val session = graphDAO.session

    session.isClosed should be(false)

  }

}
