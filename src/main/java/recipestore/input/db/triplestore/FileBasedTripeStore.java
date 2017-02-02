package recipestore.input.db.triplestore;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDBFactory;

import javax.inject.Inject;
import java.io.FileInputStream;

/**
 * Uses Jena TDB for triplestore in the local filesystem.
 * Not very scalable, but can handle upto million quads in
 * a commodity dev hardware.
 */
public class FileBasedTripeStore {

    private final String datasetName;
    private final Dataset dataset;

    @Inject
    public FileBasedTripeStore(String datasetName) {
        this.datasetName = datasetName;
        this.dataset = TDBFactory.createDataset(datasetName);
    }


    public void creataDataset(FileInputStream fileInputStream) {

        RDFDataMgr.read(dataset, fileInputStream, Lang.NQUADS);

        final Model recipeModel = dataset.getNamedModel("urn:x-arq:UnionGraph");

        final OntModel ontologyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM, recipeModel);
    }
}
