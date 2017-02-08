package recipestore.db.triplestore;

import com.complexible.stardog.api.Connection;
import com.complexible.stardog.api.ConnectionConfiguration;
import com.complexible.stardog.api.admin.AdminConnection;
import com.complexible.stardog.api.admin.AdminConnectionConfiguration;
import com.complexible.stardog.jena.SDJenaFactory;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.other.BatchedStreamRDF;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import recipestore.db.triplestore.rdfparsers.CustomRDFDataMgr;
import recipestore.db.triplestore.rdfparsers.LenientNquadParser;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.complexible.common.base.Option.create;
import static com.complexible.common.base.Options.singleton;

/**
 * Stardog database. Supports OWL 2 reasoning.
 */
@Getter
public class StardogTripleStoreDAO implements TripleStoreDAO {


    private final String datasetName;
    private final StardogConfiguration configuration;
    private Model model;
    private Connection connection;
    private Dataset dataset;

    @Inject
    public StardogTripleStoreDAO(final String datasetName, final StardogConfiguration configuration) {
        this.datasetName = datasetName;
        this.configuration = configuration;
        createModel();
    }

    @Override
    public void populate(InputStream datasetStream) {
        final ConnectionConfiguration connectionConfiguration = getConnectionConfiguration();
        try (Connection aConn = connectionConfiguration.connect()) {
            aConn.begin();
            final Model model = SDJenaFactory.createModel(aConn);
            Consumer<Quad> quadConsumer = (quad) -> model.add(model.asStatement(quad.asTriple()));
            StreamRDF sink = new BatchedStreamRDF(JenaStreamBatchHandler.createStreamBatchHandler(quadConsumer));
            CustomRDFDataMgr.parse(sink, datasetStream, LenientNquadParser.LANG);
            aConn.commit();
        } catch (Exception e) {

        }

    }

    @Override
    public Stream<Resource> getRecipeResource() {
        return null;
    }

    @Override
    public void saveAndClose() {
        if (model != null && connection != null) {
            model = null;
            connection.commit();
            connection.close();
        }
    }

    @Override
    @SneakyThrows
    public void delete(boolean clearFileSystem) {
        if (model != null && connection != null) {
            connection.admin().drop(this.datasetName);
            connection.close();
        }
    }

    private void createModel() {
        try (AdminConnection dbms = getAdminConnectionConfiguration().connect()) {

            dropDbIfAlreadyExists(dbms);

            this.connection = getConnectionConfigurationFromAdminConnection(dbms).reasoning(true)
                    .with(singleton(create("icv.reasoning.enabled"), true))
                    .with(singleton(create("icv.enabled"), true))
                    .with(singleton(create("versioning.enabled"), true))
                    .connect();

            this.model = SDJenaFactory.createModel(this.connection);
        }
    }


    private ConnectionConfiguration getConnectionConfigurationFromAdminConnection(AdminConnection dbms) {
        return this.configuration.getConfiguration().isUseEmbedded() ? dbms.memory(this.datasetName)
                .create() : dbms.disk(this.datasetName).create();
    }

    private void dropDbIfAlreadyExists(AdminConnection dbms) {
        if (dbms.list().contains(this.datasetName)) {
            dbms.drop(this.datasetName);
        }
    }


    private AdminConnectionConfiguration getAdminConnectionConfiguration() {
        final AdminConnectionConfiguration connectionConfigBuilder;
        if (!this.configuration.getConfiguration().isUseEmbedded()) {
            connectionConfigBuilder =
                    AdminConnectionConfiguration
                            .toServer(this.configuration.getConfiguration().getHostName())
                            .credentials(this.configuration.getConfiguration().getUserName(),
                                    this.configuration.getConfiguration().getPassword());
        } else {
            connectionConfigBuilder = AdminConnectionConfiguration.toEmbeddedServer()
                    .credentials("admin", "admin");
        }
        return connectionConfigBuilder;
    }

    private ConnectionConfiguration getConnectionConfiguration() {
        final ConnectionConfiguration connectionConfiguration;
        if (this.configuration.getConfiguration().isUseEmbedded()) {
            connectionConfiguration = ConnectionConfiguration
                    .to(this.datasetName);
        } else {
            connectionConfiguration = ConnectionConfiguration
                    .to(this.datasetName)
                    .server(this.configuration.getConfiguration().getHostName()).
                            credentials(this.configuration.getConfiguration().getUserName(),
                                    this.configuration.getConfiguration().getPassword());
        }
        return connectionConfiguration;
    }
}
