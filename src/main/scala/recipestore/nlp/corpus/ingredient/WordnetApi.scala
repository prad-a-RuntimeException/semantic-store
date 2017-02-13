package recipestore.nlp.corpus

import java.io.{File, InputStream}

import edu.mit.jwi.item._
import edu.mit.jwi.{IDictionary, RAMDictionary}
import org.apache.commons.io.FileUtils

import scala.collection.JavaConverters._

class WordnetApi(wordnetResource: InputStream) {

  class WordnetResource(val names: Set[String], val children: Iterable[String]) {
  }


  lazy val dict: IDictionary = {
    val file: File = File.createTempFile("wordnet", "tmp")
    FileUtils.copyInputStreamToFile(wordnetResource, file)
    val dict: IDictionary = new RAMDictionary(file)
    dict.open
    dict
  }


  def getWordnetFoodData: Map[String, WordnetResource] = {

    dict.getIndexWordIterator(POS.NOUN)
      .asScala
      .flatMap(word => word.getWordIDs.asScala)
      .filter(word => dict.getSynset(word.getSynsetID())
        .getLexicalFile()
        .getName().contains("food"))
      .map(getWordnetResource)
      .flatMap(w => w.names.map(a => Map(a -> w)))
      .flatten.toMap
  }

  private def getWordnetResource(wordId: IWordID): WordnetResource = {
    val word: IWord = dict.getWord(wordId)
    val synset: ISynset = word.getSynset
    val concepts: List[String] = synset.getWords.asScala.map(_.getLemma).toList.:+(word.getLemma)
    val children: Iterable[String] = getPointers(dict, synset, Pointer.HYPONYM)

    new WordnetResource(concepts.toSet, children)
  }

  private def getPointers(dict: IDictionary, synset: ISynset, pointer: Pointer): Iterable[String] = {
    val pointers: Iterable[ISynsetID] = synset.getRelatedSynsets(pointer).asScala
    pointers
      .flatMap(id => dict.getSynset(id).getWords.asScala.toList)
      .map(word => word.getLemma())
  }
}
