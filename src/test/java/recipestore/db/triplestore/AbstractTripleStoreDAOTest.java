package recipestore.db.triplestore;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.hamcrest.Matchers;
import org.jooq.lambda.Seq;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractTripleStoreDAOTest {

    abstract TripleStoreDAO getTripleStoreDAO();

    @Test
    public void shouldGetModelAndListStatements() {

        assertThat("Model should be present ", getTripleStoreDAO().getModel(), Matchers.notNullValue());


        final List<Statement> statements = Seq.seq(getTripleStoreDAO().getModel().listStatements()).toList();
        assertThat("Should have statements in triplestore", statements.size() > 0, Matchers.equalTo(true));

        final List<Statement> recipeStatements = statements.stream().filter(stmt -> stmt.getObject().canAs(Resource.class))
                .filter(stmt -> stmt.getObject().asResource().getURI() != null)
                .filter(stmt -> stmt.getObject().asResource().getURI().endsWith("Recipe")).collect(Collectors.toList());

        assertThat("Should get all the recipe statements ", recipeStatements.size(), Matchers.equalTo(63));

    }


    @After
    public void cleanup() {
        getTripleStoreDAO().delete(true);
    }
}
