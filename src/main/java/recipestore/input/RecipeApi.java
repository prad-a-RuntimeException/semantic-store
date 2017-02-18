package recipestore.input;


import org.apache.jena.rdf.model.Resource;
import org.jooq.lambda.Seq;
import recipestore.db.triplestore.TripleStoreDAO;

import javax.inject.Inject;
import java.io.InputStream;
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

    public Stream<Resource> getRecipeData() {
        return Seq.seq(tripleStoreDAO.getResource("http://schema.org/Recipe"));
    }

}
