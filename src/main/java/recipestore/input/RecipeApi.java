package recipestore.input;


import com.clearspring.analytics.util.Lists;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import recipestore.db.triplestore.TripleStoreDAO;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

public class RecipeApi {

    private static final Logger LOGGER = getLogger(RecipeApi.class);
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
        final Dataset dataset = tripleStoreDAO.getDataset();
        final List<String> namedGraphs
                = Lists.newArrayList();
        final Iterator<String> graphItr = dataset.listNames();
        AtomicBoolean hasNext = new AtomicBoolean(graphItr.hasNext());
        while (hasNext.get()) {
            namedGraphs.add(graphItr.next());
            try {
                hasNext.set(graphItr.hasNext());
            } catch (Exception e) {
                LOGGER.error("Failed getting named graph {}", e);
                hasNext.set(false);
            }
        }


        return namedGraphs
                .stream()
                .map(graphName -> {
                    final Model thisModel = dataset.getNamedModel(graphName);
                    return thisModel;
                }).flatMap(namedModel -> {
                    final StmtIterator stmtItr = namedModel.listStatements();
                    List<Resource> recipeResources = Lists.newArrayList();
                    while (stmtItr.hasNext()) {
                        try {
                            final Statement statement = stmtItr.next();
                            final RDFNode object = statement.getObject();
                            if (object != null && object.isResource() && object.asResource().getURI() != null
                                    && object.asResource().getURI().equals("http://schema.org/Recipe")) {
                                recipeResources.add(statement.getSubject().asResource());
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed extracting statement with exception {}", e.getMessage());
                        }
                    }

                    return recipeResources.stream();
                });


    }

}
