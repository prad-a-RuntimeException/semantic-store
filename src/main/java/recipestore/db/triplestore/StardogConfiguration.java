package recipestore.db.triplestore;

import com.google.common.base.MoreObjects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ResourceBundle;

import static com.google.common.collect.Lists.newArrayList;

public class StardogConfiguration {

    public static final Logger LOGGER = LoggerFactory.getLogger(StardogConfiguration.class);

    public static final String TRIPLESTORE_PROPERTIES = "triplestore";
    private final ResourceBundle bundle;
    @Getter
    private final Configuration configuration;

    @Getter
    @ToString
    @EqualsAndHashCode
    static class Configuration {
        private final boolean useEmbedded;
        private final String hostName;
        private final String userName;
        private final String password;

        public Configuration(boolean useEmbedded) {
            this.useEmbedded = useEmbedded;
            this.hostName = "localhost";
            this.userName = null;
            this.password = null;
        }

        public Configuration(String hostName, String userName, String password) {
            this.useEmbedded = false;
            this.hostName = hostName;
            this.userName = userName;
            this.password = password;
        }
    }

    @Inject
    public StardogConfiguration() {
        this(ResourceBundle.getBundle(TRIPLESTORE_PROPERTIES));
    }

    public StardogConfiguration(ResourceBundle inputBundle) {
        this.bundle = MoreObjects.firstNonNull(inputBundle, ResourceBundle.getBundle(TRIPLESTORE_PROPERTIES));
        if (this.bundle.containsKey("useEmbedded") && Boolean.valueOf(this.bundle.getString("useEmbedded"))) {

            LOGGER.warn("Using embedded triplestore. Only use this for testing and development");
            this.configuration = new Configuration(true);
            final boolean extraneousConfiguration =
                    newArrayList("stardog-url", "stardog-username", "stardog-password")
                            .stream()
                            .anyMatch(this.bundle::containsKey);

            if (extraneousConfiguration) {
                LOGGER.warn("Stardog configurations are ignored when setting useEmbedded to be true ");
            }

        } else {

            final boolean necessaryConfiguration =
                    newArrayList("stardog-url", "stardog-username", "stardog-password")
                            .stream()
                            .allMatch(this.bundle::containsKey);
            if (!necessaryConfiguration) {
                throw new IllegalArgumentException("Stardog credentials needs to be provided or UseEmbedded should be set to true");
            }
            this.configuration = new Configuration(this.bundle.getString("stardog-url"),
                    this.bundle.getString("stardog-username"),
                    this.bundle.getString("stardog-password"));
        }
    }


}
