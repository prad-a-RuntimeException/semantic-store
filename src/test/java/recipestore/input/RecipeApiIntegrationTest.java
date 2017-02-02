package recipestore.input;

import lombok.SneakyThrows;
import org.apache.jena.ext.com.google.common.io.Resources;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.BeforeClass;
import org.junit.Test;
import recipestore.db.triplestore.FileBasedTripeStoreDAO;

import java.io.InputStream;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class RecipeApiIntegrationTest {

    private static RecipeApi recipeApi;

    @BeforeClass
    @SneakyThrows
    public static void setup() {
        final InputStream recipeStream = Resources.getResource("test_quads.nq").openStream();
        recipeApi = new RecipeApi(new FileBasedTripeStoreDAO("int-test"), recipeStream);
        recipeApi.loadRecipe(true);
    }

    @Test
    public void shouldGetRecipeDataFromTheTripleStore() {

        final Stream<Statement> recipeData = recipeApi.getRecipeData();

        Predicate<Resource> resourceUrlPredicate
                = resource -> {
            final Statement property = resource.getProperty(resource.getModel().getProperty("http://schema.org/Recipe/url"));
            return property != null && property.getObject() != null && property.getObject().toString().contains("allrecipes");
        };

        recipeData
                .map(stmt -> stmt.getSubject().asResource())
                .filter(resourceUrlPredicate)
                .forEach(recipe -> {

                    recipe.listProperties()
                            .forEachRemaining(stmt -> {
                                System.out.println(stmt);
                            });

                });

    }


}