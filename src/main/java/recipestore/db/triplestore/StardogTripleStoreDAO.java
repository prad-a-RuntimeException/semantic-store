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
import org.openrdf.rio.RDFFormat;

import javax.inject.Inject;
import java.io.InputStream;

import static com.complexible.common.base.Option.create;
import static com.complexible.common.base.Options.singleton;

/**
 * Stardog database. Supports OWL 2 reasoning.
 */
@Getter
public class StardogTripleStoreDAO implements TripleStoreDAO {


    private final String datasetName;
    private final StardogConfiguration configuration;
    @Getter
    private Model model;
    private Connection connection;

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

            aConn.add().io()
                    .format(RDFFormat.NQUADS)
                    .stream(datasetStream);

            this.model = SDJenaFactory.createModel(this.connection);

            this.model.listStatements()
                    .forEachRemaining(stmt -> {
                        System.out.println(stmt);
                    });

            aConn.commit();
        }

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

    @Override
    public Dataset getDataset() {
        return null;
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
