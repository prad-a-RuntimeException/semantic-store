package recipestore.input

import org.apache.jena.rdf.model.{Property, Resource}

object RecipeResourceFilter {
  def getRecipeWithMinimumNumberOfRating(resource: Resource): Boolean = {
    try {
      val ratingStmts = resource.listProperties(getAggregateRatingProperty(resource))
      if (ratingStmts.hasNext()) {
        val ratingValueStmt = ratingStmts.next().getObject()
          .asResource().listProperties(getNumberOfReviewsProperty(resource))

        (if (ratingValueStmt.hasNext()) ratingValueStmt.next().getObject().asLiteral().getDouble()
        else
          0) > 10

      }
    } catch {
      case _: Throwable => false
    }
    return false
  }

  private def getAggregateRatingProperty(resource: Resource): Property = {
    return resource.getModel.getProperty("http://schema.org/Recipe/aggregateRating")
  }

  private def getNumberOfReviewsProperty(resource: Resource): Property = {
    return resource.getModel.getProperty("http://schema.org/AggregateRating/reviewCount")
  }
}