package recipestore.nlp.corpus.ingredient

import javax.inject.Inject

import com.google.inject.Guice
import org.apache.lucene.document.Document
import org.apache.lucene.index.Terms
import org.apache.lucene.misc.TermStats
import recipestore.nlp.NlpModule
import recipestore.nlp.lucene.LuceneDAO

import scala.collection.mutable


object StatsCollector {
  val recipeNlpModule = Guice.createInjector(new NlpModule(NlpModule.ingredientIndexDir))
  val wordnetNlpModule = Guice.createInjector(new NlpModule(NlpModule.wordnetIndexDir))
  val ingredientStatsCollector = recipeNlpModule.getInstance(classOf[StatsCollector])
  val wordnetStatsCollector = wordnetNlpModule.getInstance(classOf[StatsCollector])
  lazy val topIngredients: Set[String] = ingredients.map(term => term.termtext.utf8ToString()).toSet
  lazy val indexedIngredient: Map[String, Int] = topIngredients.zipWithIndex.map(tup => (tup._1, tup._2)).toMap
  val numDocs = ingredientStatsCollector.numDocs().toDouble

  def getTermsForField(id: Int, fieldName: String): Terms = ingredientStatsCollector.getTermsForField(id, fieldName)

  def getFieldValue(id: Int, fieldName: String): Any = ingredientStatsCollector.getFieldValue(id, fieldName)

  def getFieldValues(id: Int, fieldName: String): Iterable[String] = ingredientStatsCollector.getFieldValues(id, fieldName)


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

class StatsCollector @Inject()(val luceneDAO: LuceneDAO) {

  val luceneSearchApi = luceneDAO.luceneSearchAPi

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
