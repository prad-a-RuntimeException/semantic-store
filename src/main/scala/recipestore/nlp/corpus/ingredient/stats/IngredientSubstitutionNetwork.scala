package recipestore.nlp.corpus.ingredient.stats

import java.util.concurrent.atomic.AtomicInteger

import com.google.common.base.Strings.isNullOrEmpty
import recipestore.metrics.MeterWrapper
import recipestore.metrics.MetricsFactory.{get, remove}
import recipestore.nlp.corpus.ingredient.NlpStatisticsGenerator.ingredients
import recipestore.nlp.corpus.ingredient.stats.models.IngredientSubstitutionSimilarity
import recipestore.nlp.corpus.ingredient.{RecipeReviewParser, NlpStatisticsGenerator}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object IngredientSubstitutionNetwork {

  class IngredientSubstitution(val ing1: String, val ing2: String, val distance: Double) {
    override def toString = s"IngredientSubstitutionDistance($ing1, $ing2, $distance)"

    def canEqual(other: Any): Boolean = other.isInstanceOf[IngredientSubstitution]

    override def equals(other: Any): Boolean = other match {
      case that: IngredientSubstitution =>
        (that canEqual this) &&
          ing1 == that.ing1 &&
          ing2 == that.ing2
      case _ => false
    }

    override def hashCode(): Int = {
      val state = Seq(ing1, ing2)
      state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
    }
  }


  def apply(): Iterable[IngredientSubstitutionSimilarity] = {

    val meter = get("IngredientSubstitution", classOf[MeterWrapper])
    val numDocs = NlpStatisticsGenerator.numDocs

    val ingredientCountMap: Map[Int, Int] = ingredients.map(term =>
      (NlpStatisticsGenerator.indexedIngredient(term.termtext.utf8ToString()), term.docFreq))
      .toMap

    val ingredientMap: Map[String, String] = ingredients.map(term => (term.termtext.utf8ToString(), term.termtext.utf8ToString()))
      .toMap


    def fromControlledVocabulary(ing1: String): Int = {
      val mappedIngredient = ing1.split(" ")
        .find(token => ingredientMap.contains(token))
        .orNull

      if (mappedIngredient != null)
        NlpStatisticsGenerator.indexedIngredient(mappedIngredient)
      else -1

    }

    val ingredientSubstitution: Iterable[Future[Iterable[(String, String)]]] =
      (1 to (numDocs.toInt - 1))
        .map(i => (NlpStatisticsGenerator.getFieldValue(i, "id"), NlpStatisticsGenerator.getFieldValues(i, "reviews")))
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
          recipeSubstitutionMap
            .getOrElseUpdate(new IngredientSubstitution(r._1, r._2, 0),
              new AtomicInteger(0)).incrementAndGet()
        })
        case Failure(_) => null
      })

    Await.ready(Future.sequence(ingredientSubstitution), Duration.Inf)

    remove("IngredientSubstitution", classOf[MeterWrapper])

    recipeSubstitutionMap
      .filterNot(m => isNullOrEmpty(m._1.ing1) || isNullOrEmpty(m._1.ing2))
      .map(m => new IngredientSubstitutionSimilarity(fromControlledVocabulary(m._1.ing1),
        fromControlledVocabulary(m._1.ing2), m._2.get().toDouble))
      .filterNot(m => m.ing1 > 0 || m.ing2 > 0)
      .map(m => new IngredientSubstitutionSimilarity(m.ing1,
        m.ing2,
        m.distance / (ingredientCountMap.get(m.ing1).getOrElse(1).toDouble)))

  }

}
