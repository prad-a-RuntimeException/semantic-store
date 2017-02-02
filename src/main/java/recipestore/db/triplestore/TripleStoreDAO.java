package recipestore.db.triplestore;

import org.apache.jena.rdf.model.Model;

import java.io.InputStream;

public interface TripleStoreDAO {
    Model populate(InputStream fileInputStream);

    void saveAndClose();

    void delete(boolean clearFileSystem);

    Model getModel();
}
