package recipestore.input;

import lombok.SneakyThrows;
import org.apache.jena.ext.com.google.common.io.Resources;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;
import recipestore.db.triplestore.FileBasedTripleStoreDAO;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.hamcrest.MatcherAssert.assertThat;

public class RecipeApiIntegrationTest {

    private static RecipeApi recipeApi;

    @BeforeClass
    @SneakyThrows
    public static void setup() {
        final InputStream recipeStream = Resources.getResource("test_quads.nq").openStream();
        recipeApi = new RecipeApi(new FileBasedTripleStoreDAO("int-test"), recipeStream);
        recipeApi.loadRecipe(true);
    }

    @Test
    public void shouldGetRecipeDataFromTheTripleStore() {

        final List<Resource> recipeData = recipeApi.getRecipeData(resource -> RecipePredicates.filterByUrl.test(resource, "allrecipes")).collect(Collectors.toList());


        final Model singleRecipeModel = createDefaultModel().add(recipeData.get(0).listProperties().toList());
        final StringWriter out = new StringWriter();
        RDFDataMgr.write(out, singleRecipeModel, RDFFormat.RDFXML);
        System.out.println(out.toString());


        assertThat("Should have allrecipes records ", recipeData.size(), Matchers.greaterThan(1));


    }


}