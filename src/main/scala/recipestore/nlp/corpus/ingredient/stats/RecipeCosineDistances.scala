package recipestore.nlp.corpus.ingredient.stats

import com.google.common.collect.{HashMultimap, Multimap}
import recipestore.metrics.MeterWrapper
import recipestore.metrics.MetricsFactory.{get, remove}
import recipestore.nlp.corpus.ingredient.StatsCollector

import scala.collection.JavaConverters._
import scala.collection.mutable

object RecipeCosineDistances {

  def apply(): Iterable[(String, String, Double)] = {

    val termVectorMeter = get("CreateTermVector", classOf[MeterWrapper])
    val createCosineDistanceMeter = get("CosineDistance", classOf[MeterWrapper])
    val topIngredients: Set[String] = StatsCollector.ingredients
      .map(term => term.termtext.utf8ToString()).toSet

    val ingredientToRecipeInvIndex: Multimap[Int, String] = HashMultimap.create()
    val recipeTermVector: mutable.Map[String, org.apache.commons.math3.linear.RealVector] = mutable.Map()

    def ingredientCodes(ingredients: Iterable[String]): Iterable[Int] = {
      ingredients.map(ing => StatsCollector.indexedIngredient.get(ing).get)
    }

    //Create inverted index, so that we do not have to loop through every other recipe (quadratic)

    (1 to (StatsCollector.numDocs.toInt - 1))
      .map(i => (StatsCollector.getFieldValue(i, "name"),
        StatsCollector.getFieldValue(i, "id"),
        ingredientCodes(StatsCollector.allowOnlyTopIngredients(StatsCollector.getTermsForField(i, "ingredients")))))
      .foreach(tup => {
        val vector = new org.apache.commons.math3.linear.ArrayRealVector(topIngredients.size)
        termVectorMeter.poke
        tup._3.foreach(ingCode => {
          //Weight them by entropy(tf/idf)
          vector.setEntry(ingCode, 1 * StatsCollector.weightedIngredientVector.get(ingCode).get)
          ingredientToRecipeInvIndex.put(ingCode, tup._2.toString)
        })
        recipeTermVector.put(tup._2.toString, vector)
      })

    remove("CreateTermVector", classOf[MeterWrapper])


    val recipeDistance = (1 to (StatsCollector.numDocs.toInt - 1))
      .map(docId => StatsCollector.getFieldValue(docId, "id"))
      .flatMap(recipeId => {
        createCosineDistanceMeter.poke
        val thisTermVector = recipeTermVector.get(recipeId.toString).get
        thisTermVector
          .iterator()
          .asScala
          .flatMap(entry => ingredientToRecipeInvIndex.get(entry.getIndex).asScala)
          .toSeq.groupBy(str => str)
          .map(v => (v._1, v._2.size))
          //Only those who share at-least 4 ingredient
          .filter(v => v._2 > 4)
          .map(v => (recipeId.toString, v._1, recipeTermVector.get(v._1).get.dotProduct(thisTermVector)))
      })

    remove("CreatePropertyGraph", classOf[MeterWrapper])
    recipeDistance
  }

}
