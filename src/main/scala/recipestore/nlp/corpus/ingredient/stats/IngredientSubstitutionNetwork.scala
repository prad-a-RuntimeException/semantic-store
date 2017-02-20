package recipestore.nlp.corpus.ingredient.stats

import java.util.concurrent.atomic.AtomicInteger

import recipestore.metrics.MeterWrapper
import recipestore.metrics.MetricsFactory.{get, remove}
import recipestore.nlp.corpus.ingredient.stats.models.IngredientSubstitution
import recipestore.nlp.corpus.ingredient.{RecipeReviewParser, StatsCollector}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object IngredientSubstitutionNetwork {

  def apply(): Map[IngredientSubstitution, Double] = {

    val meter = get("IngredientSubstitution", classOf[MeterWrapper])
    val numDocs = StatsCollector.numDocs

    val ingredientCountMap: Map[String, Int] = StatsCollector.ingredients.map(term => (term.termtext.utf8ToString(), term.docFreq))
      .toMap

    val ingredientSubstitution: Iterable[Future[Iterable[IngredientSubstitution]]] =
      (1 to (numDocs.toInt - 1))
        .map(i => (StatsCollector.getFieldValue(i, "id"), StatsCollector.getFieldValues(i, "reviews")))
        .filter(v => v._2 != null)
        .flatMap(v => {
          println(v._2.size)
          v._2.map(review => {
            meter.poke
            Future {
              RecipeReviewParser(review)
            }
          })
        })


    val recipeSubstitutionMap = mutable.Map[IngredientSubstitution, AtomicInteger]()

    ingredientSubstitution
      .foreach(ing => ing.onComplete {
        case Success(ingredientSubstitutions) => ingredientSubstitutions.foreach(r => {
          recipeSubstitutionMap.getOrElseUpdate(r, new AtomicInteger(0)).incrementAndGet()
        })
        case Failure(t) => null
      })

    Await.ready(Future.sequence(ingredientSubstitution), Duration.Inf)
    remove("IngredientSubstitution", classOf[MeterWrapper])

    println(recipeSubstitutionMap)
    null

  }

}
