package recipestore.input;

import lombok.SneakyThrows;
import org.apache.jena.ext.com.google.common.io.Resources;
import org.apache.jena.rdf.model.Resource;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;
import recipestore.db.triplestore.FileBasedTripeStoreDAO;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;

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

        final List<Resource> recipeData = recipeApi.getRecipeData(resource -> RecipePredicates.filterByUrl.test(resource, "allrecipes")).collect(Collectors.toList());


        assertThat("Should have allrecipes records ", recipeData.size(), Matchers.greaterThan(1));


    }


}