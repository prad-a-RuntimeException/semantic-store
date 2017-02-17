package recipestore.graph

import java.util

import org.apache.jena.rdf.model.{Literal, Resource, Statement}

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.collection.mutable

object GraphVisitor {

  class Vertex(val id: String, val typeVal: String, val properties: Map[String, Object]) {


    override def toString = s"Vertex($id, $properties)"

    def canEqual(other: Any): Boolean = other.isInstanceOf[Vertex]

    override def equals(other: Any): Boolean = other match {
      case that: Vertex =>
        (that canEqual this) &&
          typeVal == that.typeVal &&
          id == that.id
      case _ => false
    }

    override def hashCode(): Int = {
      val state = Seq(typeVal, id)
      state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
    }
  }

  class Edge(val outVertex: String, val property: String, val inVertex: String) {
    override def toString = s"Edge($outVertex, $property, $inVertex)"
  }

  val resourceIdCache: Map[Resource, String] = Map()

  def getResourceID(resource: Resource): String = {
    resourceIdCache.getOrElse(resource, ((thisResource: Resource) => {
      if (thisResource.isAnon)
        thisResource.getId.toString
      else
        thisResource.getURI
    }).apply(resource))
  }

  def traverse[A, B](resource: Resource, visitVertexFn: Function[Vertex, A],
                     visitEdgeFn: Function[Edge, B]): Unit = {

    val stack: util.Stack[Resource] = new util.Stack[Resource]
    stack.add(resource)
    val handled: mutable.Set[String] = mutable.Set()
    while (!stack.isEmpty) {
      val thisResource = stack.pop()
      val propertyList: List[Statement] =
        try {
          thisResource.listProperties().asScala.toList
        } catch {
          case _: UnsupportedOperationException => List()
        }
      if (propertyList.nonEmpty) {

        val propertiesAndEdges: (Seq[Statement], Seq[Statement]) = propertyList
          .toStream
          .partition(stmt => stmt.getObject.canAs(classOf[Literal]))

        val thisVertex: Vertex = createVertex(thisResource, propertiesAndEdges._1)
        handled.add(thisVertex.id)
        visitVertexFn.apply(thisVertex)

        propertiesAndEdges._2
          .filter(stmt => !handled.contains(getResourceID(stmt.getObject.asResource())))
          .foreach(e => {
            handled.add(getResourceID(e.getObject.asResource()))
            val thisEdge = new Edge(getResourceID(thisResource), e.getPredicate.getLocalName,
              getResourceID(e.getObject.asResource()))
            visitEdgeFn.apply(thisEdge)
            stack.add(e.getObject.asResource())
          })
      }
    }

  }

  def getResourceType(resource: Resource): String = resource.listProperties().toList.asScala
    .filter(stmt => stmt.getPredicate.getLocalName.equals("type"))
    .filter(stmt => stmt.getObject != null)
    .filter(stmt => stmt.getObject.canAs(classOf[Resource]))
    .filter(stmt => stmt.getObject.asResource().getLocalName != null)
    .map(stmt => stmt.getObject.asResource())
    .map(obj => obj.getLocalName)
    .filterNot(name => name == null || name.contains("w3"))
    .find(_ != null).orNull

  private def createVertex(resource: Resource, propertyList: Seq[Statement]) = {


    val propertyValue: Map[String, AnyRef] = propertyList
      .map(stmt => (stmt.getPredicate.getLocalName, stmt.getObject.asLiteral().getValue))
      .filter((tuple: (String, AnyRef)) => tuple._1 != null && tuple._2 != null)
      .groupBy(tuple => tuple._1)
      .map(tuple => tuple._1 -> tuple._2.map(predVal => predVal._2))
      .map(tuple => if (tuple._2.size > 1) tuple._1 -> tuple._2.toList else tuple._1 -> tuple._2.head)

    new Vertex(getResourceID(resource), getResourceType(resource), propertyValue)
  }
}
