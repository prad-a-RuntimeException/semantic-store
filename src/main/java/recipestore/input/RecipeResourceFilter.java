package recipestore.input;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;

import java.util.function.Predicate;

public class RecipeResourceFilter {

    public static Predicate<Resource> getRecipeWithMinimumNumberOfRating
            = (resource) -> {
        try {
            final StmtIterator ratingStmts = resource.listProperties(getAggregateRatingProperty(resource));
            if (ratingStmts.hasNext()) {
                final StmtIterator ratingValueStmt = ratingStmts.next().getObject()
                        .asResource().listProperties(getNumberOfReviewsProperty(resource));

                return (ratingValueStmt.hasNext() ? ratingValueStmt.next().getObject().asLiteral().getDouble()
                        : 0) >= 10;

            }
        } catch (Exception e) {
            return false;
        }
        return false;
    };

    private static Property getAggregateRatingProperty(Resource resource) {
        return resource.getModel().getProperty("http://schema.org/Recipe/aggregateRating");
    }

    private static Property getNumberOfReviewsProperty(Resource resource) {
        return resource.getModel().getProperty("http://schema.org/AggregateRating/reviewCount");
    }


}
