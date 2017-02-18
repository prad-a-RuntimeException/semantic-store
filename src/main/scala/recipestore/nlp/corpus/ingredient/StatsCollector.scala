package recipestore.nlp.corpus.ingredient

import javax.inject.Inject

import com.google.common.collect.{HashMultimap, Multimap}
import com.google.inject.Guice
import org.apache.lucene.document.Document
import org.apache.lucene.index.Terms
import org.apache.lucene.misc.TermStats
import recipestore.metrics.MeterWrapper
import recipestore.metrics.MetricsFactory._
import recipestore.misc.CrossProduct.Cross
import recipestore.nlp.NlpModule
import recipestore.nlp.lucene.LuceneSearchApi

import scala.collection.JavaConverters._
import scala.collection.mutable


object StatsCollector {
  val recipeNlpModule = Guice.createInjector(new NlpModule(NlpModule.ingredientIndexDir))
  val wordnetNlpModule = Guice.createInjector(new NlpModule(NlpModule.wordnetIndexDir))
  val ingredientStatsCollector = recipeNlpModule.getInstance(classOf[StatsCollector])
  val wordnetStatsCollector = wordnetNlpModule.getInstance(classOf[StatsCollector])
  lazy val topIngredients: Set[String] = ingredients.map(term => term.termtext.utf8ToString()).toSet
  lazy val indexedIngredient: Map[String, Int] = topIngredients.zipWithIndex.map(tup => (tup._1, tup._2)).toMap
  val numDocs = ingredientStatsCollector.numDocs().toDouble


  def main(args: Array[String]): Unit = {
    val score = ingredientSubstitutionScore()

  }

  //Weight ingredient based on tf/idf
  def weightedIngredientVector: Map[Int, Double] = {

    def getTFIDF(termStats: TermStats): Double = {
      numDocs / termStats.docFreq.toDouble
    }

    val weightedIngredientVector: Map[Int, Double] = ingredients
      .map(ingredient => (indexedIngredient.get(ingredient.termtext.utf8ToString()).get, getTFIDF(ingredient)))
      .toMap
    weightedIngredientVector
  }

  def ingredientSubstitutionScore(): Unit = {

    val meter = get("IngredientSubstitution", classOf[MeterWrapper])
    val numDocs = ingredientStatsCollector.numDocs()

    (1 to (numDocs.toInt - 1))
      .map(i => (ingredientStatsCollector.getFieldValue(i, "id"), ingredientStatsCollector.getFieldValues(i, "reviews")))
      .filter(v => v._2 != null)
      .foreach(v => {
        v._2.foreach(review => {
          meter.poke
        })
      })

    println(meter.status)
    remove("IngredientSubstitution", classOf[MeterWrapper])


  }

  def recipeCosineDistances(): Iterable[(String, String, Double)] = {

    val termVectorMeter = get("CreateTermVector", classOf[MeterWrapper])
    val createCosineDistanceMeter = get("CosineDistance", classOf[MeterWrapper])
    val topIngredients: Set[String] = ingredients.map(term => term.termtext.utf8ToString()).toSet

    val ingredientToRecipeInvIndex: Multimap[Int, String] = HashMultimap.create()
    val recipeTermVector: mutable.Map[String, org.apache.commons.math3.linear.RealVector] = mutable.Map()

    def ingredientCodes(ingredients: Iterable[String]): Iterable[Int] = {
      ingredients.map(ing => indexedIngredient.get(ing).get)
    }

    //Create inverted index, so that we do not have to loop through every other recipe (quadratic)

    (1 to (numDocs.toInt - 1))
      .map(i => (ingredientStatsCollector.getFieldValue(i, "name"),
        ingredientStatsCollector.getFieldValue(i, "id"),
        ingredientCodes(allowOnlyTopIngredients(ingredientStatsCollector.getTermsForField(i, "ingredients")))))
      .foreach(tup => {
        val vector = new org.apache.commons.math3.linear.ArrayRealVector(topIngredients.size)
        termVectorMeter.poke
        tup._3.foreach(ingCode => {
          //Weight them by entropy(tf/idf)
          vector.setEntry(ingCode, 1 * weightedIngredientVector.get(ingCode).get)
          ingredientToRecipeInvIndex.put(ingCode, tup._2.toString)
        })
        recipeTermVector.put(tup._2.toString, vector)
      })

    remove("CreateTermVector", classOf[MeterWrapper])


    val recipeDistance = (1 to (numDocs.toInt - 1))
      .map(docId => ingredientStatsCollector.getFieldValue(docId, "id"))
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

  def allowOnlyTopIngredients(terms: Terms): mutable.Set[String] = {
    val filteredTerms: mutable.Set[String] = mutable.Set()
    try {
      val iterator = terms.iterator()
      var term = iterator.next

      while (term != null) {
        if (topIngredients.contains(term.utf8ToString())) {
          filteredTerms.add(term.utf8ToString())
        }
        term = iterator.next()
      }
      filteredTerms
    } catch {
      case _: Throwable => filteredTerms
    }
  }

  class IngredientComplementDistance(val ing1: String, val ing2: String, val distance: Double) extends Ordered[IngredientComplementDistance] {

    override def compare(that: IngredientComplementDistance): Int = this.distance.compare(that.distance)

    override def toString = s"IngredientComplementDistance($ing1, $ing2, $distance)"
  }


  def ingredientComplementScore(): Iterable[IngredientComplementDistance] = {

    val meter = get("ComplementScore", classOf[MeterWrapper])
    val numDocs = ingredientStatsCollector.numDocs()


    //Order agnostic tuple
    class Pair(val first: String, val second: String) {
      private val values = Set(first, second)

      def canEqual(other: Any): Boolean = other.isInstanceOf[Pair]

      override def equals(other: Any): Boolean = other match {
        case that: Pair =>
          (that canEqual this) &&
            values == that.values
        case _ => false
      }

      override def hashCode(): Int = {
        val state = Seq(values)
        state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
      }
    }



    val ingredientCount = ingredients.map(term => (term.termtext.utf8ToString(), term.docFreq)).toMap

    def calculatePmi(t: ((String, String), Int), ingredientCount: Map[String, Int]) = {
      val pOfAAnB: Double = Math.log(t._2.toDouble /
        (ingredientCount(t._1._1).toDouble * ingredientCount(t._1._2).toDouble))
      new IngredientComplementDistance(t._1._1, t._1._2, pOfAAnB)
    }


    val complementDistance = (1 to (numDocs.toInt - 1))
      .map(i => allowOnlyTopIngredients(ingredientStatsCollector.getTermsForField(i, "ingredients")))
      .flatMap(i => i.cross(i).filterNot(f => f._1.contains(f._2) || (f._2.contains(f._1)))
        .map(i => {
          meter.poke
          new Pair(i._1, i._2)
        })
        .toSeq.distinct)
      .groupBy(pair => pair)
      .map(pair => Map((pair._1.first, pair._1.second) -> pair._2.size))
      .flatten
      .map(t => calculatePmi(t, ingredientCount))
      .toList.sorted

    remove("ComplementScore", classOf[MeterWrapper])
    complementDistance
  }


  lazy val ingredients: Array[TermStats] = {

    val termsInRecipeIngredients = ingredientStatsCollector.getIngredientsByFrequency(10000, "ingredients")
    val ingredientsFromControlledVocabulary = wordnetStatsCollector.getIngredientsByFrequency(10000, "synonyms")
      .map(term => term.termtext.utf8ToString())
      .toSet

    termsInRecipeIngredients
      .filter(term => ingredientsFromControlledVocabulary.contains(term.termtext.utf8ToString()))
      .filter(term => term.docFreq > 10)
  }
}

class StatsCollector @Inject()(val luceneSearchApi: LuceneSearchApi) {

  def getIngredientsByFrequency(numTerms: Int, fieldName: String): Array[TermStats] = {
    luceneSearchApi.topFrequencyWords(numTerms, fieldName)
  }

  def numDocs(): Long = {
    luceneSearchApi.indexReader.numDocs()
  }

  def doc(id: Int): Document = {
    luceneSearchApi.indexReader.document(id)
  }

  def getTermsForField(id: Int, fieldName: String): Terms = {
    luceneSearchApi.indexReader.getTermVector(id, fieldName)
  }

  def getFieldValue(id: Int, fieldName: String): Any = {
    doc(id).get(fieldName)
  }

  def getFieldValues(id: Int, fieldName: String): Iterable[String] = {
    doc(id).getFields(fieldName).map(f => f.stringValue())
  }

}
