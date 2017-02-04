package recipestore.db.triplestore;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;

import static org.hamcrest.MatcherAssert.assertThat;

public class StardogConfigurationTest {


    @Test
    public void shouldGetEmbeddedConfiguration() {


        ResourceBundle bundle = createBundle(ImmutableMap.of("useEmbedded", "true"));


        final StardogConfiguration stardogConfiguration = new StardogConfiguration(bundle);

        final StardogConfiguration.Configuration configuration = stardogConfiguration.getConfiguration();

        assertThat("Should return embedded configuration ", configuration, Matchers.equalTo(
                new StardogConfiguration.Configuration(true)
        ));


    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailWithoutValidConfiguration() {


        ResourceBundle bundle = createBundle(ImmutableMap.of("stardog-url", "some", "stardog", "username", "stardog-password", "password"));


        final StardogConfiguration stardogConfiguration = new StardogConfiguration(bundle);

        final StardogConfiguration.Configuration configuration = stardogConfiguration.getConfiguration();

        assertThat("Should return embedded configuration ", configuration, Matchers.equalTo(
                new StardogConfiguration.Configuration("some", "username", "password")
        ));


    }

    @Test
    public void shouldGetServerConfiguration() {


        ResourceBundle bundle = createBundle(ImmutableMap.of("stardog-url", "some", "stardog-username", "username", "stardog-password", "password"));


        final StardogConfiguration stardogConfiguration = new StardogConfiguration(bundle);

        final StardogConfiguration.Configuration configuration = stardogConfiguration.getConfiguration();

        assertThat("Should return embedded configuration ", configuration, Matchers.equalTo(
                new StardogConfiguration.Configuration("some", "username", "password")
        ));


    }

    private ResourceBundle createBundle(Map<String, String> keyValues) {
        return new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                if (keyValues.containsKey(key)) {
                    return keyValues.get(key);
                } else {
                    throw new RuntimeException("Code should be more defensive to check for keys before retrieval");
                }
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.enumeration(keyValues.keySet());
            }
        };
    }

}