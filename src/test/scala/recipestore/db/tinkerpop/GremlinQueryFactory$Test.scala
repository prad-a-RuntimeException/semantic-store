package recipestore.db.tinkerpop

import com.github.javafaker.Faker
import org.scalatest.Matchers

class GremlinQueryFactory$Test extends org.scalatest.FunSuite with Matchers {

  val faker = new Faker()

  test("Should create valid vertex creation query") {
    val vertexLabel = faker.beer().name()
    val propertyMap = Map("Key1" -> "Value1")
    val addVertexScript = GremlinQueryFactory.addVertexScript(vertexLabel, propertyMap)

    addVertexScript should equal(s"g.addV(label,'$vertexLabel','Key1', Value1)")
  }

  test("Should create valid vertex creation query without any parameter") {
    val vertexLabel = faker.beer().name()
    val propertyMap = null
    val addVertexScript = GremlinQueryFactory.addVertexScript(vertexLabel, propertyMap)

    addVertexScript should equal(s"g.addV(label,'$vertexLabel')")
  }

  test("Should create valid edge creation query") {
    val edgeLabel = faker.beer().name()
    val propertyMap = Map("Key1" -> "Value1")
    val addEdgeScript = GremlinQueryFactory.addEdgeScript(edgeLabel, propertyMap)

    addEdgeScript should equal(s"def v1 = g.V(id1).next() def v2 = g.V(id2).next() v1.addEdge('$edgeLabel', v2, List())")
  }

}
