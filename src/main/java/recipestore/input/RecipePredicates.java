package recipestore.input;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.function.BiPredicate;

public class RecipePredicates {


    public static BiPredicate<Resource, String> filterByUrl = (resource, pattern) -> {
        final Statement property = resource.getProperty(resource.getModel().getProperty("http://schema.org/Recipe/url"));
        return property != null && property.getObject() != null && property.getObject().toString().contains(pattern);
    };

}
