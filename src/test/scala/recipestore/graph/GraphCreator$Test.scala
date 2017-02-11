package recipestore.graph

import org.apache.spark.sql.{DataFrame, Row}
import org.graphframes.GraphFrame
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import recipestore.input.DaggerInputComponent

import scala.collection.immutable.Seq

class GraphCreator$Test extends FunSuite with BeforeAndAfter {

  var graphCreator: GraphCreator = _
  before {
    val recipeApi = DaggerInputComponent.builder().build().getRecipeApi
    val graphDirectory = DaggerGraphComponent.builder().build().getGraphDirectory
    graphCreator = new GraphCreator(recipeApi, graphDirectory.get())
    graphCreator.write(10)
  }

  test("Should Persist the GraphFrame and reload it") {

    val reloadedGraphFrame: GraphFrame = (() => graphCreator.loadFromFile()).apply()
    val vertices: DataFrame = reloadedGraphFrame.vertices


    def assertValue() = {
      val count: Long = vertices.filter(row => row.getString(row.fieldIndex("type")).equals("Recipe"))
        .count()
      assert(count.toInt == 10)
      vertices.foreach(row => {
        def assertRow(row: Row) = {
          object matcher extends Matchers {
            def checkVal(row: Row) = {
              val schemaField: Seq[String] = row.schema.fields.map(_.name).toList
              schemaField should contain allOf("ratingValue", "author", "name", "description", "mainEntityOfPage", "recipeInstructions", "id", "ingredients", "type", "reviewCount", "recipeYield")
            }
          }
          matcher.checkVal(row)
        }

        assertRow(row)
      })
    }

    assertValue()

  }

}
