package recipestore.db.triplestore;

import org.apache.jena.rdf.model.Resource;
import org.hamcrest.Matchers;
import org.jooq.lambda.Seq;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public abstract class AbstractTripleStoreDAOTest {

    abstract TripleStoreDAO getTripleStoreDAO();

    @Test
    public void shouldGetModelAndListStatements() {

        final List<Resource> statements = Seq.seq(getTripleStoreDAO().getResource("http://schema.org/Recipe")).toList();
        assertThat("Should have statements in triplestore", statements.size() > 0, Matchers.equalTo(true));

        assertThat("Should get all the recipe statements ", statements.size(), greaterThan(10));

    }


    @After
    public void cleanup() {
        getTripleStoreDAO().delete(true);
    }
}
