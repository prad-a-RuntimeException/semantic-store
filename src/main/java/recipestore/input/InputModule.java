package recipestore.input;

import dagger.Module;
import dagger.Provides;
import recipestore.db.triplestore.FileBasedTripleStoreDAO;
import recipestore.db.triplestore.TripleStoreDAO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Module
public class InputModule {

    private static final String DATASET_FILE_LOC = "data/output.nq";


    @Provides
    public static TripleStoreDAO providesTripleStoreDAO() {
        return new FileBasedTripleStoreDAO("recipe");
    }

    @Provides
    public static InputStream providesDatasetStream() {
        try {
            return Files.newInputStream(Paths.get(DATASET_FILE_LOC), StandardOpenOption.READ);
        } catch (IOException e) {
            throw new RuntimeException("Failed initializing module. Input quad file is mandatory");
        }
    }

}
