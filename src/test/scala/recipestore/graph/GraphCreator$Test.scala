package recipestore.graph

import com.google.inject.{Guice, Injector}
import org.apache.spark.sql.{DataFrame, Row}
import org.graphframes.GraphFrame
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

import scala.collection.immutable.Seq

class GraphCreator$Test extends FunSuite with BeforeAndAfter {

  var graphCreator: GraphCreator = _
  var graphLoader: GraphLoader = _
  var inputModule: Injector = Guice.createInjector(new GraphModule)
  before {
    graphCreator = inputModule.getInstance(classOf[GraphCreator])
    graphLoader = inputModule.getInstance(classOf[GraphLoader])
    graphCreator.write(-1)
  }

  ignore("Should Persist the GraphFrame and reload it") {

    val reloadedGraphFrame: GraphFrame = (() => graphLoader.loadFromFile()).apply()
    val vertices: DataFrame = reloadedGraphFrame.vertices
    val edges: DataFrame = reloadedGraphFrame.edges


    def assertValue() = {
      val count: Long = vertices.filter(row => Option(row.getString(row.fieldIndex("type"))).getOrElse("")
        .equals("Recipe"))
        .count()
      assert(count.toInt >= 10)


      object matcher extends Matchers {

        def getRowVal(row: Row, field: String) = Option(row.getString(row.fieldIndex(field))).getOrElse("")

        def getRowVals(row: Row, fields: String*) = fields.map(getRowVal(row, _))

        def checkSchema(row: Row) = {
          val schemaField: Seq[String] = row.schema.fields.map(_.name).toList
          schemaField should contain allOf("ratingValue", "author", "name", "description", "mainEntityOfPage",
            "recipeInstructions", "id", "ingredients", "type", "reviewCount")
        }

        def checkReviewsConnectedToRecipe(edge: Row) = {
          val schemaField: Seq[String] = edge.schema.fields.map(_.name).toList
          schemaField should contain allOf("src", "dst")
        }

        def checkReviewParameters(reviewVertex: Row) = {
          if (getRowVal(reviewVertex, "type").equals("Review")) {
            val reviewValues = getRowVals(reviewVertex, "reviewBody", "author")
            reviewValues.foreach(_ should not be empty)
          }
        }
      }
      vertices.foreach(matcher.checkSchema(_))
      vertices.foreach(matcher.checkReviewParameters(_))

      edges.filter("relationship = 'review'")
        .foreach(matcher.checkReviewsConnectedToRecipe(_))


    }

    assertValue()

  }

}
