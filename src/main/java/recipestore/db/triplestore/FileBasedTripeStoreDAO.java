package recipestore.db.triplestore;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import javax.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.apache.jena.tdb.TDBFactory.createDataset;

/**
 * Uses Jena TDB for triplestore in the local filesystem.
 * Not very scalable, but can handle upto million quads in
 * a commodity dev hardware.
 */
@Getter
public class FileBasedTripeStoreDAO implements TripleStoreDAO {

    private static final String BASE_LOCATION = "triple_store";

    private String datasetName;
    private Dataset dataset;
    private Model model;

    @Inject
    public FileBasedTripeStoreDAO(final String datasetName) {
        this.datasetName = datasetName;
        createModel();
    }

    private void createModel() {
        File file = new File(BASE_LOCATION);
        if (!file.exists() && !file.isDirectory()) {
            final boolean mkdir = file.mkdir();
            if (!mkdir) {
                throw new RuntimeException("Creation of directory failed ");
            }
        }
        this.dataset = createDataset(getFileLocation.get());
        this.model = dataset.getNamedModel("urn:x-arq:UnionGraph");
    }

    private final Supplier<String> getFileLocation = () -> format("%s/%s", BASE_LOCATION, this.datasetName);

    @Override
    public void populate(InputStream datasetStream) {
        if (model == null || model.isClosed()) {
            createModel();
        }
        RDFDataMgr.read(dataset, datasetStream, Lang.NQUADS);
    }


    @Override
    public void saveAndClose() {
        if (model != null && dataset != null) {
            model.commit();
            model.close();
            dataset.close();
        }
    }

    @Override
    @SneakyThrows
    public void delete(boolean clearFileSystem) {
        if (model != null && dataset != null) {
            model.removeAll();
            saveAndClose();
        }
        if (clearFileSystem)
            FileUtils.deleteDirectory(new File(getFileLocation.get()));
    }
}
