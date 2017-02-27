package recipestore.db.tinkerpop

import java.util

import com.google.common.base.{MoreObjects, Strings}
import com.google.common.collect.Sets
import recipestore.db.tinkerpop.models._

import scala.collection.JavaConverters._
import scala.collection.mutable

object DseSchemaCreator {
  def getPropertySchemaScript(property: DSEProperty): String = {
    val cardinality = if (property.isMultiple) "multiple" else "single"
    s"schema.propertyKey('${property.label}').${property.propertyType}().$cardinality().ifNotExists().create()"
  }

  private def getVertexSchemaScripts(vertex: DSEGraphVertex): List[String] = {
    val propertyCreationScripts: List[String] = vertex.getProperties
      .map(p => getPropertySchemaScript(p))

    val properties = vertex.getProperties
      .map(p => p.label)
      .map(p => "'" + p + "'")
      .mkString(",")
    val vertexCreationScript: String = s"schema.vertexLabel('${vertex.getLabel}').properties($properties).ifNotExists().create()"

    propertyCreationScripts.:+(vertexCreationScript)
  }

  def mergeVertices(graphVertices: List[DSEGraphVertex]): List[DSEGraphVertex] = {
    graphVertices.filter(vertex => vertex != null)
      .groupBy(g => g)
      .map(v => v._2.reduce((mem, curr) => {
        val dseProperties = mem.getProperties.++(curr.getProperties).distinct
        val dseGraphEdges = mem.getEdges.++(curr.getEdges)
        val dsePropertyIndices = mem.getPropertyIndices
        new DSEGraphVertex(mem.getLabel, dseProperties, dseGraphEdges, dsePropertyIndices) {
        }
      })).toList
  }

  def getEdgeSchemaScripts(edge: DSEGraphEdge): List[String] = {
    val edgeProperties = if (edge.getProperties == null) List() else edge.getProperties
    val propertyCreationScripts = edgeProperties.map(e => getPropertySchemaScript(e))


    val edgeCardinality = if (edge.isMultiple) "multiple" else "single"
    val properties = edgeProperties.map(p => p.label)
      .map(prop => "'" + prop + "'").mkString(",")
    val edgeCreationScript =
      if (Strings.isNullOrEmpty(properties)) {
        s"schema.edgeLabel('${edge.getLabel}').$edgeCardinality().ifNotExists().create()"
      } else {
        s"schema.edgeLabel('${edge.getLabel}').$edgeCardinality().properties($properties).ifNotExists().create()"
      }

    val edgeConnection
    = s"schema.edgeLabel('${edge.getLabel}').connection(${String.format("'%s','%s'", edge.getOutVertex().getLabel, edge.getInVertex().getLabel)}).add()"

    propertyCreationScripts.::(edgeCreationScript).::(edgeConnection)
  }

  private def getGraphIndexScripts(vertexLabel: String, indices: List[DSEPropertyIndex]): List[String] = {
    indices
      .flatMap(index => {
        var indexSubType: String = null
        var indexType: String = null
        var indexName: String = null
        if (index.indexType.equals(DSEIndexType.StringSearch)) {
          indexType = "search"
          indexSubType = ".asString()"
          indexName = "search"
        } else if (index.indexType.equals(DSEIndexType.TextSearch)) {
          indexSubType = ".asText()"
          indexType = "search"
          indexName = "search"
        } else {
          indexSubType = ""
          indexType = index.indexType.toString().toLowerCase()
          indexName = null
        }
        index.properties.map(p => {
          if (indexName == null) {
            indexName = s"${p}Index"
          }
          s"schema.vertexLabel('$vertexLabel').index('$indexName').$indexType().by('$p')$indexSubType.ifNotExists().add()"
        })
      })
  }

  def createSchemaThroughTraversal(vertices: List[DSEGraphVertex]): List[String] = {
    val vertexMap = vertices.groupBy(v => v)
    mergeVertices(vertices).flatMap(vertex => createSchemaThroughTraversal(vertex, vertexMap))
  }

  def createSchemaThroughTraversal(vertex: DSEGraphVertex, vertexMap: Map[DSEGraphVertex, List[DSEGraphVertex]]): List[String] = {
    val handledVertices: Set[DSEGraphVertex] = Set()
    val handledEdges: Set[DSEGraphEdge] = Set()
    val verticesToVisit: mutable.Stack[DSEGraphVertex] = mutable.Stack[DSEGraphVertex]()
    val schemaCreationScripts: util.LinkedHashSet[String] = Sets.newLinkedHashSet[String]()
    verticesToVisit.push(vertex)
    while (!verticesToVisit.isEmpty) {
      {
        val thisVertex: DSEGraphVertex = verticesToVisit.pop
        schemaCreationScripts.addAll(getVertexSchemaScripts(thisVertex).asJava)
        schemaCreationScripts.addAll(getGraphIndexScripts(thisVertex.getLabel.toString, thisVertex.getPropertyIndices).asJava)
        //perform vertex visiting function
        for (dseGraphEdge <- thisVertex.getEdges) {
          val first: DSEGraphVertex = if (vertexMap.contains(dseGraphEdge.getInVertex()))
            vertexMap.get(dseGraphEdge.getInVertex()).get.apply(0)
          else null
          var inVertex: DSEGraphVertex = null
          try {
            inVertex = MoreObjects.firstNonNull(first, dseGraphEdge.getInVertex())
          }
          catch {
            case e: Exception => {
              inVertex = null
            }
          }
          if (inVertex != null) {
            //Handle edge function
            if (!handledEdges.contains(dseGraphEdge)) {
              if (!handledVertices.contains(inVertex)) {
                schemaCreationScripts.addAll(getVertexSchemaScripts(inVertex).asJava)
                schemaCreationScripts.addAll(getGraphIndexScripts(inVertex.getLabel.toString, inVertex.getPropertyIndices).asJava)
              }
              schemaCreationScripts.addAll(getEdgeSchemaScripts(dseGraphEdge).asJava)
              handledEdges.+(dseGraphEdge)
            }
            if (!handledVertices.contains(inVertex)) {
              handledVertices.+(inVertex)
              verticesToVisit.push(inVertex)
            }
          }
        }
      }
    }
    schemaCreationScripts.asScala.toList
  }
}