package recipestore.input;


import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.jooq.lambda.Seq;
import recipestore.db.triplestore.TripleStoreDAO;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class RecipeApi {

    private final TripleStoreDAO tripleStoreDAO;
    private final InputStream datasetStream;

    @Inject
    public RecipeApi(TripleStoreDAO tripleStoreDAO, InputStream datasetStream) {
        this.tripleStoreDAO = tripleStoreDAO;
        this.datasetStream = datasetStream;
    }


    public void loadRecipe(boolean forceCreate) {
        if (forceCreate)
            tripleStoreDAO.delete(true);
        tripleStoreDAO.populate(datasetStream);
    }

    @Deprecated
    public Model get() {
        return tripleStoreDAO.getModel();
    }

    public Stream<Resource> getRecipeData(Predicate<Resource> resourceUrlPredicate) {
        final Model model = tripleStoreDAO.getModel();

        return Seq.seq(model.listStatements())
                .filter(stmt -> stmt.getObject().canAs(Resource.class))
                .filter(stmt -> stmt.getObject().asResource().getURI() != null)
                .filter(stmt -> stmt.getObject().asResource().getURI().equals("http://schema.org/Recipe"))
                .map(stmt -> stmt.getSubject().asResource())
                .filter(resourceUrlPredicate);


    }

}
