package recipestore.db.triplestore;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.io.InputStream;
import java.util.stream.Stream;

public interface TripleStoreDAO {
    void populate(InputStream fileInputStream);

    Stream<Resource> getRecipeResource();

    void saveAndClose();

    void delete(boolean clearFileSystem);

    Model getModel();

    Dataset getDataset();
}
