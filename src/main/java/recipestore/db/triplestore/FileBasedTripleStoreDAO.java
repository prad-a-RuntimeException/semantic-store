package recipestore.db.triplestore;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.other.BatchedStreamRDF;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipestore.db.triplestore.rdfparsers.CustomRDFDataMgr;
import recipestore.db.triplestore.rdfparsers.LenientNquadParser;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.String.format;
import static org.apache.jena.tdb.TDBFactory.createDataset;
import static org.apache.jena.tdb.TDBFactory.createDatasetGraph;
import static recipestore.db.triplestore.QuadsBatchHandler.createStreamBatchHandler;

/**
 * Uses Jena TDB for triplestore in the local filesystem.
 * Not very scalable, but can handle upto million quads in
 * a commodity dev hardware.
 */
@Getter
public class FileBasedTripleStoreDAO implements TripleStoreDAO {

    public static final Logger LOGGER = LoggerFactory.getLogger(FileBasedTripleStoreDAO.class);
    private static final String BASE_LOCATION = "triple_store";

    private final String datasetName;
    private Dataset dataset;
    private Consumer<Quad> quadConsumer;


    @Inject
    public FileBasedTripleStoreDAO(final @Named("datasetName") String datasetName) {
        this.datasetName = datasetName;
        initializeJenaModels();


    }

    private void initializeJenaModels() {
        File file = new File(BASE_LOCATION);
        if (!file.exists() && !file.isDirectory()) {
            final boolean mkdir = file.mkdir();
            if (!mkdir) {
                throw new RuntimeException("Creation of directory failed ");
            }
        }
        final String fileLocation = getFileLocation.apply(this.datasetName);
        this.dataset = createDataset(fileLocation);

        final DatasetGraph datasetGraph = createDatasetGraph(fileLocation);
        quadConsumer = (quad) -> {
            datasetGraph.add(quad);
        };
    }

    public final static Function<String, String> getFileLocation = (datasetName
    ) -> format("%s/%s", BASE_LOCATION, datasetName);

    @Override
    public void populate(InputStream datasetStream) {

        if (dataset == null) {
            initializeJenaModels();
        }
        try {
            StreamRDF sink = new BatchedStreamRDF(createStreamBatchHandler(quadConsumer));
            CustomRDFDataMgr.parse(sink, datasetStream, LenientNquadParser.LANG);
        } catch (Exception e) {
            LOGGER.warn("Possible bad data in the input triple ", e);
            saveAndClose();
        }
    }

    private Predicate<RDFNode> isRecipeResource = (rdfNode) ->
            rdfNode != null &&
                    rdfNode.isResource() &&
                    rdfNode.asResource().getURI() != null &&
                    rdfNode.asResource().getURI().equals("http://schema.org/Recipe");


    @Override
    public void saveAndClose() {
        if (dataset != null) {
            try {
                dataset.commit();
                dataset.close();
            } catch (Exception e) {
                LOGGER.warn("Failed cleaning up Triplestore file system");
            }
        }
    }

    @Override
    @SneakyThrows
    public void delete(boolean clearFileSystem) {
        if (dataset != null) {
            dataset.close();
            saveAndClose();
        }
        if (clearFileSystem)
            FileUtils.deleteDirectory(new File(getFileLocation.apply(this.datasetName)));
    }

    @Override
    public Iterator<Resource> getResource(String resourceUri) {

        Function<String, Resource> getResource = (graphName) -> {
            if (graphName == null) return null;
            final Model model = getDataset().getNamedModel(graphName);
            final ExtendedIterator<Resource> recipeItr = model
                    .listStatements(null, null, model.getResource(resourceUri))
                    .mapWith(stmt -> stmt.getSubject());
            if (recipeItr.hasNext()) {
                return recipeItr.next().asResource();
            } else {
                return null;
            }
        };
        final Iterator<String> graphNames = getDataset().listNames();

        if (!graphNames.hasNext()) return null;
        final AtomicReference<String> graphName = new AtomicReference<>(graphNames.next());


        return new Iterator<Resource>() {
            Resource graphResource = null;

            @Override
            public boolean hasNext() {
                try {
                    final boolean hasNextValueInGraph
                            = (graphResource = getResource.apply(graphName.get())) != null;
                    graphName.set(graphNames.hasNext() ? graphNames.next() : null);
                    return hasNextValueInGraph;
                } catch (Exception e) {
                    graphResource = null;
                    return false;
                } finally {

                }
            }

            @Override
            public Resource next() {
                return graphResource;
            }
        };


    }

}
