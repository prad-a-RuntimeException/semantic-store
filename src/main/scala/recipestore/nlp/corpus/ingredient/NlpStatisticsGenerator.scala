package recipestore.nlp.corpus.ingredient

import javax.inject.Inject

import com.google.inject.Guice.createInjector
import org.apache.lucene.document.Document
import org.apache.lucene.index.Terms
import org.apache.lucene.misc.TermStats
import org.apache.spark.sql.Row.fromSeq
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row, SaveMode}
import org.slf4j.{Logger, LoggerFactory}
import recipestore.graph.SparkFactory
import recipestore.nlp.NlpModule
import recipestore.nlp.corpus.ingredient.stats.{IngredientComplementNetwork, IngredientSubstitutionNetwork, RecipeNetwork}
import recipestore.nlp.lucene.LuceneDAO

import scala.collection.JavaConverters._
import scala.collection.mutable


object NlpStatisticsGenerator extends SparkFactory {
  private def nlpModule = (indexDir: String) => new NlpModule(indexDir)

  private val logger: Logger = LoggerFactory.getLogger(NlpStatisticsGenerator.getClass)

  def apply(graphDir: String, ingredientComplementSimilarityEdges: String, ingredientSubstitutionSimilarityEdges: String,
            codedIngredientVertices: String, recipeToCodedIngredientEdges: String, cosineDistanceEdges: String): Unit = {

    val edgeSchema = StructType(Seq("src", "dst", "relationship")
      .zip(Seq(DoubleType, DoubleType, DoubleType))
      .map(s => new StructField(s._1, s._2, false)))

    val ingredientComplementDistanceDF = sqlContext.createDataFrame(IngredientComplementNetwork()
      .map(i => {
        println(i)
        fromSeq(Seq(i.ing1, i.ing2, i.distance))
      })
      .toList.asJava, edgeSchema)

    ingredientComplementDistanceDF.write.mode(SaveMode.Overwrite)
      .parquet(s"$graphDir/$ingredientComplementSimilarityEdges ")

    logger.info("Done writing IngredientComplementSimilarityEdges ")

    val ingredientSubstitutionNetworkDF = sqlContext.createDataFrame(IngredientSubstitutionNetwork()
      .map(i => fromSeq(Seq(i.ing1, i.ing2, i.distance))).toList.asJava,
      edgeSchema)

    ingredientSubstitutionNetworkDF.write.mode(SaveMode.Overwrite)
      .parquet(s"$graphDir/$ingredientSubstitutionSimilarityEdges ")

    logger.info("Done writing ingredientSubstitutionDistanceEdges ")


    val recipeNetwork = RecipeNetwork()

    //Write the coded ingredients to it's own DF
    val ingredientVerticesDF: DataFrame = sqlContext.createDataFrame(indexedIngredient
      .map(m => fromSeq(Seq(m._2, m._1))).toList.asJava,
      StructType(Seq(new StructField("id", IntegerType), new StructField("name", StringType))))

    ingredientVerticesDF.write.mode(SaveMode.Overwrite).parquet(s"$graphDir/$codedIngredientVertices")

    logger.info("Done writing codedIngredientVertices")

    val recipeToCodedIngredientDF = sqlContext.createDataFrame(recipeNetwork._2.asMap().asScala
      .flatMap(k => k._2.asScala
        .map(v => fromSeq(Seq(k._1, v, 0)))).toList.asJava, StructType(Seq("src", "dst", "relationship")
      .zip(Seq(StringType, IntegerType, IntegerType))
      .map(s => new StructField(s._1, s._2, false))))

    recipeToCodedIngredientDF.write.mode(SaveMode.Overwrite).parquet(s"$graphDir/$recipeToCodedIngredientEdges")

    logger.info("Done writing recipeToCodedIngredientEdges")

    val cosineDistances: Iterable[Row] = recipeNetwork._1
      .map(s => fromSeq(Seq(s.recipe1, s.recipe2, s.similarity)))
    val cosineDistanceDF = sqlContext.createDataFrame(cosineDistances.toList.asJava,
      StructType(Seq("src", "dst", "relationship")
        .zip(Seq(StringType, StringType, DoubleType))
        .map(s => new StructField(s._1, s._2, false))))
    cosineDistanceDF.write.mode(SaveMode.Overwrite).parquet(s"$graphDir/$cosineDistanceEdges")

    logger.info("Done writing cosineDistanceEdges")
  }

  val module = (indexDir: String) => createInjector(nlpModule(indexDir)).getInstance(classOf[NlpStatisticsGenerator])
  val ingredientStatsCollector = module(NlpModule.ingredientIndexDir)
  val wordnetStatsCollector = module(NlpModule.wordnetIndexDir)
  lazy val ingredients: Array[TermStats] = {
    val termsInRecipeIngredients = ingredientStatsCollector.getIngredientsByFrequency(10000, "ingredients")
    val ingredientsFromControlledVocabulary = wordnetStatsCollector.getIngredientsByFrequency(10000, "synonyms")
      .map(term => term.termtext.utf8ToString())
      .toSet

    termsInRecipeIngredients
      .filter(term => ingredientsFromControlledVocabulary.contains(term.termtext.utf8ToString()))
      .filter(term => term.docFreq > 10)
  }
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


}

class NlpStatisticsGenerator @Inject()(val luceneDAO: LuceneDAO) {

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
