package recipestore.nlp

import java.util.Properties

import com.twitter.storehaus.cache.{MapCache, Memoize, MutableCache}
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}

import scala.collection.JavaConverters._

/**
  * A Stanfordcore-nlp facade
  */
object NlpPipeline {

  private val pipelineCache: MutableCache[List[String], StanfordCoreNLP] = MapCache.empty[List[String], StanfordCoreNLP].toMutable()

  private def getStandfordCoreNlpPipeline = Memoize(pipelineCache)(_getStanfordCoreNlpPipeline)

  private def _getStanfordCoreNlpPipeline(pipelines: List[String]): StanfordCoreNLP = {
    val props = new Properties
    props.put("annotators", pipelines.mkString(","))
    new StanfordCoreNLP(props)
  }

  private val defaultProcesses = List("tokenize", "ssplit", "pos", "lemma", "regexner")

  def apply(input: String, processes: List[String] = defaultProcesses) = {
    val document = new Annotation(input)
    getStandfordCoreNlpPipeline(processes)
      .annotate(document)

    document.get(classOf[CoreAnnotations.SentencesAnnotation])
      .asScala
      .flatMap(_.get(classOf[TokensAnnotation]).asScala)
  }


}
