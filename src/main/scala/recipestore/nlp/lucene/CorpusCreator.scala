package recipestore.nlp.lucene

import com.google.inject.Guice
import org.apache.lucene.analysis.Analyzer
import org.apache.spark.sql.DataFrame
import recipestore.nlp.NlpModule
import recipestore.nlp.NlpModule.ingredientIndexDir
import recipestore.nlp.corpus.ingredient.WordnetCorpusFactory
import recipestore.nlp.corpus.recipe.IngredientCorpusFactory

object CorpusCreator {

  def main(args: Array[String]): Unit = {

    createWordnetCorpus
    createRecipeIngredientCorpus
  }

  private def createRecipeIngredientCorpus = {

    val ingredientCorpusFactory = () => {
      val graphModule = Guice.createInjector(new NlpModule(indexDir = ingredientIndexDir))
      graphModule.getInstance(classOf[IngredientCorpusFactory])
    }
    val luceneWriterApi = () => {
      val graphModule = Guice.createInjector(new NlpModule(indexDir = ingredientIndexDir))
      graphModule.getInstance(classOf[LuceneDAO]).luceneWriteApi
    }
    _createRecipeCorpus(() => {
      ingredientCorpusFactory().datasets
    },
      () => ingredientCorpusFactory().analyzer,
      () => luceneWriterApi(),
      ingredientIndexDir)
  }

  private def createWordnetCorpus(): LuceneSearchApi = {
    val nlpModule = Guice.createInjector(new NlpModule(indexDir = NlpModule.wordnetIndexDir))
    val luceneWriteApi = nlpModule.getInstance(classOf[LuceneDAO]).luceneWriteApi
    val wordnetCorpusFactory = nlpModule.getInstance(classOf[WordnetCorpusFactory])
    luceneWriteApi.write(wordnetCorpusFactory.document)
    nlpModule.getInstance(classOf[LuceneDAO]).luceneSearchAPi
  }


  /**
    * Spark module expects everything to be serializable, we try to divorce the spark
    * runnable to the instance by creating (anon) functions.
    *
    * @param recipeVertices
    * @param analyzer
    * @param indexDir
    */
  def _createRecipeCorpus(recipeVertices: () => DataFrame, analyzer: () => Analyzer, luceneWriteApi: () => LuceneWriter, indexDir: String) = {

    val recipeDF = recipeVertices()
    val accumulator = recipeDF.sparkSession.sparkContext.longAccumulator("RecipeIndexLoaderCount")
    recipeDF
      .repartition(1)
      .foreachPartition(partition => {
        val writerApi = luceneWriteApi()
        partition.foreach(row => {
          if (accumulator.value % 1000 == 0) {
            println(s"Done with $accumulator.value records")
          }
          val valuesMap: Map[String, Nothing] = row.getValuesMap(Seq("id", "name", "ingredients", "reviews"))
          accumulator.add(1)
          writerApi.write(valuesMap)
        })
        writerApi.commit()
      })


  }

}
