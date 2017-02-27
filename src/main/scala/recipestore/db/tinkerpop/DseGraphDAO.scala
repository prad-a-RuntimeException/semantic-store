package recipestore.db.tinkerpop

import com.datastax.driver.core.{HostDistance, PoolingOptions}
import com.datastax.driver.dse.graph.{GraphOptions, GraphResultSet, SimpleGraphStatement}
import com.datastax.driver.dse.{DseCluster, DseSession}
import com.google.common.collect.Maps
import com.twitter.storehaus.cache.{MapCache, Memoize, MutableCache}
import org.slf4j.{Logger, LoggerFactory}
import recipestore.ResourceLoader
import recipestore.ResourceLoader.{Resource, get}
import recipestore.misc.OptionConvertors._
import recipestore.misc.RichListenableFuture._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DseGraphDAO {
  val logger: Logger = LoggerFactory.getLogger(DseGraphDAO.getClass)
  val sessionCache: MutableCache[String, DseSession] = MapCache.empty[String, DseSession].toMutable()
  val clusterName = get.apply(Resource.tinkerpop, "cluster_name").toOption.getOrElse("")
  val isProductionCode = get.apply(Resource.tinkerpop, "is_production_mode")
    .toOption
    .getOrElse("false")
    .toBoolean

  val dseSession
  = Memoize(sessionCache)(initSession)

  private def _getGraphDAO(graphName: String) = {
    lazy val graphDAO = new DseGraphDAO(clusterName, graphName, isProductionCode)
    graphDAO
  }

  private def initSession(graphName: String): DseSession = {
    this.synchronized {
      val dseHost: String = ResourceLoader.get.apply(ResourceLoader.Resource.tinkerpop, "dse_host").orElseGet(null)
      val poolingOptions = new PoolingOptions();
      poolingOptions
        .setConnectionsPerHost(HostDistance.LOCAL, 4, 10)
        .setMaxRequestsPerConnection(HostDistance.LOCAL, 32768)
        .setMaxRequestsPerConnection(HostDistance.REMOTE, 2000)
        .setConnectionsPerHost(HostDistance.REMOTE, 2, 4)

      val dseSession: DseSession = DseCluster.builder.addContactPoint(dseHost)
        .withPoolingOptions(poolingOptions)
        .withGraphOptions(new GraphOptions()
          .setGraphName(graphName))
        .build.newSession
      dseSession.executeGraph(new SimpleGraphStatement(String.format("system.graph('%s').ifNotExists().create()", graphName)).setSystemQuery)
      //      if (isProductionMode) dseSession.executeGraph("schema.config().option('graph.schema_mode').set('Production')")
      dseSession
    }
  }

  def executeAsync(dseSession: DseSession, query: String, propertyMap: Map[String, AnyRef] = Map()): Future[GraphResultSet] = {
    val javaPropertyMap: java.util.Map[String, Object] = Maps.newHashMap()
    propertyMap.foreach(k => javaPropertyMap.put(k._1, k._2 match {
      case _: Iterable[Any] => k._2.asInstanceOf[Iterable[Any]].toList.asJava
      case _ => k._2
    }))
    val result = dseSession.executeGraphAsync(query, javaPropertyMap).asScala
    result.onFailure({
      case e: Throwable => {
        logger.trace(s"execution of query $query failed,with message ${e.getMessage}")
      }
    })
    result
  }

  def execute(dseSession: DseSession, query: String, propertyMap: Map[String, AnyRef] = Map()): GraphResultSet = {
    val javaPropertyMap: java.util.Map[String, Object] = Maps.newHashMap()
    propertyMap.foreach(k => javaPropertyMap.put(k._1, k._2 match {
      case _: Iterable[Any] => k._2.asInstanceOf[Iterable[Any]].toList.asJava
      case _ => k._2
    }))
    val result = dseSession.executeGraph(query, javaPropertyMap)
    result
  }

  def drop(dseSession: DseSession, graphName: String) = {
    dseSession.execute(s"system.graph($graphName).drop()")
  }

  def clearSchema(dseSession: DseSession) = {
    dseSession.executeGraph("schema.clear()")
  }

}


class DseGraphDAO(clusterName: String, graphName: String, isProductionMode: Boolean) extends Serializable {
  val logger: Logger = LoggerFactory.getLogger(classOf[DseGraphDAO])
  @volatile var session: DseSession = null
}
