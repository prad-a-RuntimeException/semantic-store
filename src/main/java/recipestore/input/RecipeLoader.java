package recipestore.input;


import org.apache.jena.rdf.model.Model;
import recipestore.db.triplestore.TripleStoreDAO;

import javax.inject.Inject;
import java.io.InputStream;

public class RecipeLoader {

    private final TripleStoreDAO tripleStoreDAO;
    private final InputStream datasetStream;

    @Inject
    public RecipeLoader(TripleStoreDAO tripleStoreDAO, InputStream datasetStream) {
        this.tripleStoreDAO = tripleStoreDAO;
        this.datasetStream = datasetStream;
    }


    public Model loadRecipe() {
        return tripleStoreDAO.populate(datasetStream);
    }

    public Model get() {
        return tripleStoreDAO.getModel();
    }

}
