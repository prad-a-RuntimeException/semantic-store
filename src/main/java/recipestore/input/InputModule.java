package recipestore.input;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import recipestore.ResourceLoader;
import recipestore.db.triplestore.FileBasedTripleStoreDAO;
import recipestore.db.triplestore.TripleStoreDAO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static recipestore.ResourceLoader.Resource.triplestore;

public class InputModule extends AbstractModule {

    private static final String DATASET_FILE_LOC = ResourceLoader.get.apply(triplestore,
            "input-file").orElse(null);


    @Provides
    public static String providesDatasetName() {
        return "recipe";
    }


    @Provides
    public static InputStream providesDatasetStream() {
        try {
            return Files.newInputStream(Paths.get(DATASET_FILE_LOC), StandardOpenOption.READ);
        } catch (IOException e) {
            throw new RuntimeException("Failed initializing module. Input quad file is mandatory");
        }
    }

    @Override
    protected void configure() {
        bind(TripleStoreDAO.class).to(FileBasedTripleStoreDAO.class);
    }
}
