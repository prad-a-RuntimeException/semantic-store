package recipestore.nlp.corpus.ingredient.stats

import java.util

import com.google.common.collect.{HashMultimap, Multimap}
import recipestore.metrics.MeterWrapper
import recipestore.metrics.MetricsFactory.{get, remove}
import recipestore.nlp.corpus.ingredient.NlpStatisticsGenerator
import recipestore.nlp.corpus.ingredient.stats.models.RecipeCosineSimilarity

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Calculates the cosine similarity between recipes and also
  * provides connection between encoded ingredients to the
  */
object RecipeNetwork {

  def apply(): (Iterable[RecipeCosineSimilarity], Multimap[String, Int]) = {

    val termVectorMeter = get("CreateTermVector", classOf[MeterWrapper])
    val createCosineDistanceMeter = get("CosineDistance", classOf[MeterWrapper])
    val cosinePairMeter = get("CosinePairMeter", classOf[MeterWrapper])
    val topIngredients: Set[String] = NlpStatisticsGenerator.ingredients
      .map(term => term.termtext.utf8ToString()).toSet

    val ingredientToRecipeInvIndex: Multimap[Int, String] = HashMultimap.create()
    val recipeToIngredientMap: Multimap[String, Int] = HashMultimap.create()
    val recipeTermVector: mutable.Map[String, org.apache.commons.math3.linear.RealVector] = mutable.Map()

    def ingredientCodes(ingredients: Iterable[String]): Iterable[Int] = {
      ingredients.map(ing => NlpStatisticsGenerator.indexedIngredient.get(ing).get)
    }

    //Create inverted index, so that we do not have to loop through every other recipe (quadratic)
    (1 to (NlpStatisticsGenerator.numDocs.toInt - 1))
      .map(i => (NlpStatisticsGenerator.getFieldValue(i, "name"),
        NlpStatisticsGenerator.getFieldValue(i, "id"),
        ingredientCodes(NlpStatisticsGenerator.allowOnlyTopIngredients(NlpStatisticsGenerator.getTermsForField(i, "ingredients")))))
      .filter(_._2 != null)
      .foreach(tup => {
        val vector = new org.apache.commons.math3.linear.ArrayRealVector(topIngredients.size)
        termVectorMeter.poke
        tup._3.foreach(ingCode => {
          //Weight them by entropy(tf/idf)
          val weight: Double = NlpStatisticsGenerator.weightedIngredientVector.get(ingCode).get
          if (weight > 5) {
            vector.setEntry(ingCode, 1 * weight)
            ingredientToRecipeInvIndex.put(ingCode, tup._2.toString)
            recipeToIngredientMap.put(tup._2.toString, ingCode)
          }
        })
        recipeTermVector.put(tup._2.toString, vector)
      })

    remove("CreateTermVector", classOf[MeterWrapper])


    val recipeDistance = (1 to (NlpStatisticsGenerator.numDocs.toInt - 1))
      .map(docId => NlpStatisticsGenerator.getFieldValue(docId, "id"))
      .filter(_ != null)
      .flatMap(recipeId => {
        createCosineDistanceMeter.poke
        val thisTermVector = recipeTermVector.get(recipeId.toString).get
        val ingredients: util.Collection[Int] = recipeToIngredientMap.get(recipeId.toString)
        ingredients
          .asScala
          .flatMap(entry => ingredientToRecipeInvIndex.get(entry).asScala.filter(r => recipeId.toString.compareTo(r) > 0))
          .toSeq
          .groupBy(str => str)
          .map(v => (v._1, v._2.size))
          //Only those who share at-least 4 ingredient
          .filter(v => (v._2.toDouble / ingredients.size().toDouble) * 100 > 70)
          .map(v => {
            cosinePairMeter.poke
            new RecipeCosineSimilarity(recipeId.toString, v._1,
              recipeTermVector.get(v._1).get.cosine(thisTermVector))
          })
      })

    remove("CreatePropertyGraph", classOf[MeterWrapper])
    remove("CosinePairMeter", classOf[MeterWrapper])
    (recipeDistance, recipeToIngredientMap)
  }

}
