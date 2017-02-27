package recipestore.db.triplestore.tinkerpop

import org.scalatest.{FunSuite, Matchers}
import recipestore.db.tinkerpop.DseGraphDAO

class DseGraphDAOTest extends FunSuite with Matchers {

  ignore("Should be able to open connection to local dse") {
    val graphDAO = new DseGraphDAO("local", "Recipe", false)

    val session = graphDAO.session

    session.isClosed should be(false)

  }

}
