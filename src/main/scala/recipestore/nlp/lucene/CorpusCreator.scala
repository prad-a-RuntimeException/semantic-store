package recipestore.nlp.lucene

import com.google.inject.Guice
import org.apache.lucene.analysis.Analyzer
import org.apache.spark.sql.DataFrame
import recipestore.nlp.NlpModule
import recipestore.nlp.corpus.ingredient.WordnetCorpusFactory
import recipestore.nlp.corpus.recipe.IngredientCorpusFactory

object CorpusCreator {

  private val wordnetIndexDir = "wordnet"
  private val ingredientIndexDir = "ingredient"


  def main(args: Array[String]): Unit = {


    val factory = () => {
      val graphModule = Guice.createInjector(new NlpModule(ingredientIndexDir))
      graphModule.getInstance(classOf[IngredientCorpusFactory])
    }
    createRecipeCorpus(() => {
      factory.apply().datasets
    }, () => factory.apply().analyzer,
      ingredientIndexDir)
  }

  def createWordnetCorpus(): LuceneSearchApi = {
    val nlpModule = Guice.createInjector(new NlpModule(wordnetIndexDir))
    val luceneWriteApi = nlpModule.getInstance(classOf[LuceneWriteApi])
    val wordnetCorpusFactory = nlpModule.getInstance(classOf[WordnetCorpusFactory])
    luceneWriteApi.write(wordnetCorpusFactory.document)
    nlpModule.getInstance(classOf[LuceneSearchApi])
  }


  /**
    * Spark module expects everything to be serializable, we try to divorce the spark
    * runnable to the instance by creating (anon) functions.
    *
    * @param frame
    * @param analyzer
    * @param indexDir
    */
  def createRecipeCorpus(frame: () => DataFrame, analyzer: () => Analyzer, indexDir: String) = {
    def getLuceneWriterApi = {
      val nlpModule = Guice.createInjector(new NlpModule(indexDir, analyzer.apply()))
      val luceneWriteApi = nlpModule.getInstance(classOf[LuceneWriteApi])
      luceneWriteApi
    }

    def writeRow(valuesMap: Map[String, Nothing]) = {
      val luceneWriteApi = getLuceneWriterApi
      luceneWriteApi.write(valuesMap)
    }

    frame.apply()
      .repartition(1)
      .foreachPartition(partition => {
        val writerApi = getLuceneWriterApi
        partition.foreach(row => {
          val valuesMap: Map[String, Nothing] = row.getValuesMap(Seq("id", "name", "ingredients"))
          writerApi.write(valuesMap)
        })
        writerApi.commit()
      })


  }

}
