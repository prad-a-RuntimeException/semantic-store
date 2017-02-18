package recipestore.db.triplestore;

import org.apache.jena.rdf.model.Resource;

import java.io.InputStream;
import java.util.Iterator;

public interface TripleStoreDAO {

    void populate(InputStream fileInputStream);

    void saveAndClose();

    void delete(boolean clearFileSystem);

    Iterator<Resource> getResource(String resourceUri);


}
