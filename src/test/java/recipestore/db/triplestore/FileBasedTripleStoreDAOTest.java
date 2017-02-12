package recipestore.db.triplestore;

import java.io.IOException;
import java.io.InputStream;

import static com.google.common.io.Resources.getResource;

public class FileBasedTripleStoreDAOTest extends AbstractTripleStoreDAOTest {

    private static final String TEST_FILE = "recipe.nq";
    private static FileBasedTripleStoreDAO tripleStoreDAO;

    static {
        final InputStream recipeStream;
        try {
            recipeStream = getResource(TEST_FILE).openStream();
            tripleStoreDAO = new FileBasedTripleStoreDAO("test");
            tripleStoreDAO.populate(recipeStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed initializing query ", e);
        }
    }


    @Override
    TripleStoreDAO getTripleStoreDAO() {
        return tripleStoreDAO;
    }
}