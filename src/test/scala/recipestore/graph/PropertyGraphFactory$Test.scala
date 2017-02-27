package recipestore.graph

import com.google.common.io.Resources
import org.apache.jena.rdf.model.{ModelFactory, Resource}
import org.apache.spark.sql.Row
import org.graphframes.GraphFrame
import org.scalatest.{FunSuite, Matchers}

import scala.collection.JavaConverters._
import scala.collection.mutable


class PropertyGraphFactory$Test extends FunSuite with Matchers {


  test("Should create PropertyGraph(Spark GraphFrames) from Semantic resource") {
    val recipeModel = ModelFactory.createDefaultModel.read(Resources.getResource("sample_recipe.rdf").openStream, "http://schema.org/Recipe")
    val sampleRecipeUnderTest: mutable.Seq[Resource] = recipeModel.listStatements.toList.asScala.map(stmt => stmt.getSubject)
    sampleRecipeUnderTest.size should be > 0
    val graph: GraphFrame = PropertyGraphFactory.createGraph(sampleRecipeUnderTest.head)


    val recipeRow: Row = graph.vertices.filter(row => row.getString(row.fieldIndex("type")).equals("Recipe"))
      .first()

    val fields: Seq[String] = recipeRow.schema.fields.map(field => field.name)
    fields.toList should contain allOf("id",
      "type",
      "ingredients",
      "recipeYield",
      "mainEntityOfPage",
      "author",
      "description",
      "name",
      "recipeInstructions"
    )

  }

}
