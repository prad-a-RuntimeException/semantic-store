package recipestore.db.tinkerpop

import com.google.common.base.Strings

/**
  * DSE as of when this class is created, still lacks Gremlin Fluent Api.Hence
  * a bunch of helper classes for some common DSE Graph operations.
  *
  */
object GremlinQueryFactory {

  def addVertexScript(label: String, propertyMap: Map[String, AnyRef]): String = {
    val propertyChain = if (propertyMap == null) ""
    else propertyMap
      .filterNot(entry => entry._1.toLowerCase().equals("label"))
      .filterNot(_._2 == null)
      .filterNot(e => e._1.startsWith("id"))
      //Not a mistake.We are not interpolating actual value, rather
      // use the binding mechanism available in DSE graph api
      .map(e => s"'${e._1}', ${e._1}")
      .mkString(",")

    val propertyValuesString = if (Strings.isNullOrEmpty(propertyChain))
      ""
    else s",$propertyChain"

    s"g.addV(label,'$label'$propertyValuesString)"
  }

  def addEdgeScript(edgeLabel: String, propertyMap: Map[String, AnyRef] = Map()) = {
    val propertyChain = if (propertyMap == null || propertyMap.size == 0) ""
    else
      "," + propertyMap
        .filterNot(e => e._1.equals("id"))
        .filterNot(e => e._1.toLowerCase().equals("label"))
        .filter(_._2 != null)
        .map(e => s"'${e._1}', ${e._1}")
        .mkString(",")

    s"""def v1 = g.V().has('resourceId',id1).next()\n def v2 = g.V().has('resourceId',id2).next()\n v1.addEdge('$edgeLabel', v2 $propertyChain)"""

  }

}
