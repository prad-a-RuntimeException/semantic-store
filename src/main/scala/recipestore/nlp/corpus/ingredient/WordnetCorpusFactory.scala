package recipestore.nlp.corpus.ingredient

import com.google.common.collect.{HashMultimap, Multimap}
import recipestore.nlp.corpus.{WordnetApi, WordnetModel}
import recipestore.nlp.lucene.CorpusSource

import scala.collection.JavaConverters._
import scala.collection.immutable.Iterable

/**
  * Creates the lucene database of the Wordnet corpus
  */
object WordnetCorpusFactory {
  var wordnetCorpusFactory: WordnetCorpusFactory = null

  def apply(): WordnetCorpusFactory = {
    if (this.wordnetCorpusFactory == null) {
      this.wordnetCorpusFactory = new WordnetCorpusFactory(new WordnetApi())
    }
    this.wordnetCorpusFactory
  }
}

class WordnetCorpusFactory(val wordnetApi: WordnetApi) extends CorpusSource {

  lazy val wordnetData: Map[String, WordnetModel] = wordnetApi.getWordnetFoodData

  //Find only phrases with more than one token
  lazy val phrases: Iterable[String] = wordnetData.map(tup => tup._2.names.+(tup._1)).flatten
    .filter(d => (d.count((c) => c == '_') > 0))
    .map(d => d.replace('_', ' '))

  lazy val data = {
    //removing all possible duplicate entries from the synonym map
    val wordnetResourceMap: Multimap[String, WordnetModel] = HashMultimap.create()
    wordnetData.map(tup => (tup._2.names.+(tup._1), tup._2))
      .foreach(tuple => tuple._1.foreach(k => wordnetResourceMap.put(k, tuple._2)))

    def findDuplicates = {
      wordnetResourceMap.asMap().asScala
        .filter(e => e._2.size() > 1)
    }

    val mergedModels: Map[String, WordnetModel] = findDuplicates
      .flatMap(e => {
        val mergedModel: WordnetModel = e._2.asScala.reduce((mem, curr) =>
          new WordnetModel(mem.names.++(curr.names), mem.children.toSet.++(curr.children)))
        mergedModel.names.map(n => Map(n -> mergedModel))
      })
      .flatten
      .toMap

    val mergedModelMap: Set[WordnetModel] = mergedModels.map(m => m._2).toSet
    val handledNames = mergedModelMap.flatMap(s => s.names)
    mergedModelMap.++(wordnetResourceMap
      .asMap()
      .asScala
      .filterNot(t => handledNames.contains(t._1))
      .flatMap(_._2.asScala))
      .map(s => (s.names.head -> s.names))
      .toMap
  }

  lazy val document: Iterable[Map[String, AnyRef]] = data.map(d => Map("name" -> d._1, "synonyms" -> d._2))

  //Only synonyms with more than one value in the synonymy set
  lazy val synonyms = data.filter(doc => doc._2.size > 1)


}
