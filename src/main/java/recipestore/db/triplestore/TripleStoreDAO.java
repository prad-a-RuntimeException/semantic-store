package recipestore.db.triplestore;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public interface TripleStoreDAO {

    void populate(InputStream fileInputStream);

    Stream<Map<String, RDFNode>> runQuery(String sparqlQuery, Set<String> variables);

    void saveAndClose();

    void delete(boolean clearFileSystem);

    Iterator<Resource> getResource(String resourceUri);


}
