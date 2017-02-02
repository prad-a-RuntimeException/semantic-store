package recipestore.db.triplestore;

import lombok.SneakyThrows;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.hamcrest.Matchers;
import org.jooq.lambda.Seq;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.io.Resources.getResource;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileBasedTripeStoreDAOTest {

    public static final String TEST_FILE = "test_quads.nq";
    private static FileBasedTripeStoreDAO tripleStoreDAO;

    @SneakyThrows
    @BeforeClass
    public static void setup() {
        final InputStream recipeStream = getResource(TEST_FILE).openStream();
        tripleStoreDAO = new FileBasedTripeStoreDAO("test");
        tripleStoreDAO.populate(recipeStream);
    }

    @Test
    public void shouldGetModelAndListStatements() {

        assertThat("Model should be present ", tripleStoreDAO.getModel(), Matchers.notNullValue());

        final List<Statement> statements = Seq.seq(tripleStoreDAO.getModel().listStatements()).toList();
        assertThat("Should have statements in triplestore", statements.size() > 0, Matchers.equalTo(true));

        final List<Statement> recipeStatements = statements.stream().filter(stmt -> stmt.getObject().canAs(Resource.class))
                .filter(stmt -> stmt.getObject().asResource().getURI() != null)
                .filter(stmt -> stmt.getObject().asResource().getURI().endsWith("Recipe")).collect(Collectors.toList());

        recipeStatements.forEach(System.out::println);


    }


    @AfterClass
    public static void cleanup() {
        tripleStoreDAO.delete(true);
    }


}