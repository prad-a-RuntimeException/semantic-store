package recipestore.db.triplestore;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.other.StreamRDFBatchHandler;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Consumer;

import static org.slf4j.LoggerFactory.getLogger;

public class QuadsBatchHandler implements StreamRDFBatchHandler {


    public static QuadsBatchHandler createStreamBatchHandler(Consumer<Quad> quadConsumer) {
        return new QuadsBatchHandler(quadConsumer);
    }

    private static final Logger LOGGER = getLogger(QuadsBatchHandler.class);
    private final Consumer<Quad> quadConsumer;


    @Inject
    public QuadsBatchHandler(Consumer<Quad> quadConsumer) {
        this.quadConsumer = quadConsumer;
    }


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
        if (RecipePredicates.filterByUrl.test(uri)) {
            LOGGER.trace("For graph {} and subject {}, found quads  {}", currentGraph, currentSubject,
                    quads.size());
            quads.forEach(quadConsumer);
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
}
