package recipestore.db.triplestore;

import com.google.common.collect.ImmutableList;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.jooq.lambda.Seq;
import recipestore.metrics.AddMeter;

import java.io.InputStream;
import java.util.Iterator;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

public interface TripleStoreDAO {

    void populate(InputStream fileInputStream);

    void saveAndClose();

    void delete(boolean clearFileSystem);

    Model getModel();

    default Stream<Resource> getRecipeResource() {
        final Iterator<Resource> recipeItr = getModel()
                .listStatements(null, null, getModel().getResource("http://schema.org/Recipe"))
                .mapWith(stmt -> stmt.getSubject());
        final ImmutableList.Builder<Resource> recipeListBuilder = ImmutableList.builder();
        try {
            recipeItr.forEachRemaining(recipe -> addData(recipeListBuilder, recipe));
        } catch (Exception e) {
            getLogger(TripleStoreDAO.class).warn("Recipe statement iterator failing {} ", e.getLocalizedMessage());
        }
        return Seq.seq(recipeListBuilder.build());
    }

    @AddMeter
    default void addData(ImmutableList.Builder<Resource> recipeListBuilder, final Resource resource) {
        recipeListBuilder.add(resource);
    }
}
