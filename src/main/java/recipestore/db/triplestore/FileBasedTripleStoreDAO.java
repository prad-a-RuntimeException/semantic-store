package recipestore.db.triplestore;

import com.codahale.metrics.Meter;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.other.BatchedStreamRDF;
import org.apache.jena.riot.other.StreamRDFBatchHandler;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.tdb.TDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipestore.db.triplestore.rdfparsers.CustomRDFDataMgr;
import recipestore.db.triplestore.rdfparsers.LenientNquadParser;
import recipestore.metrics.MetricsFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.apache.jena.tdb.TDBFactory.createDataset;
import static recipestore.metrics.MetricsFactory.getMetricFactory;

/**
 * Uses Jena TDB for triplestore in the local filesystem.
 * Not very scalable, but can handle upto million quads in
 * a commodity dev hardware.
 */
@Getter
public class FileBasedTripleStoreDAO implements TripleStoreDAO {

    public static final Logger LOGGER = LoggerFactory.getLogger(FileBasedTripleStoreDAO.class);
    private static final String BASE_LOCATION = "triple_store";

    private String datasetName;
    @Getter
    private Dataset dataset;
    private Model model;
    private DatasetGraph graph;

    @Inject
    public FileBasedTripleStoreDAO(final String datasetName) {
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
        final Meter meter = getMetricFactory().initializeMeter("TripleStorePopulate");
        if (model == null || model.isClosed()) {
            createModel();
        }

        try {
            AtomicInteger count = new AtomicInteger(0);
            final StreamRDFBatchHandler streamRDFBatchHandler = new StreamRDFBatchHandler() {

                @Override
                public void start() {
                    LOGGER.info("Starting nquad batch processing");
                }

                @Override
                public void batchTriples(Node currentSubject, List<Triple> triples) {

                }

                @Override
                public void batchQuads(Node currentGraph, Node currentSubject, List<Quad> quads) {
                    final String uri = currentGraph.getURI().toLowerCase();
                    if (uri.contains("allrecipes.com") || uri.contains("allrecipes.co.uk")) {
                        meter.mark();
                        LOGGER.trace("handled count {} ", count.incrementAndGet());
                        LOGGER.trace("For graph {} and subject {}, found quads  {}", currentGraph, currentSubject,
                                quads.size());
                        quads.forEach(graph::add);
                    }
                }

                @Override
                public void base(String base) {

                }

                @Override
                public void prefix(String prefix, String iri) {

                }

                @Override
                public void finish() {
                    LOGGER.info("Quad batch processing done");
                }
            };
            StreamRDF sink = new BatchedStreamRDF(streamRDFBatchHandler);
            CustomRDFDataMgr.parse(sink, datasetStream, LenientNquadParser.LANG);
        } catch (Exception e) {
            LOGGER.warn("Possible bad data in the input triple ", e);
            saveAndClose();
        } finally {
            MetricsFactory.getMetricFactory().stopMeter("TripleStorePopulate");
        }
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

    @Override
    public Model getModel() {
        return model;
    }
}
