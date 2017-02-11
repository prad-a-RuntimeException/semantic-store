package recipestore;

import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.Test;
import recipestore.ResourceLoader.Resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.io.Resources.getResource;
import static java.nio.file.Paths.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jooq.lambda.Seq.of;

public class ResourceLoaderTest {

    @Test
    @SneakyThrows
    public void shouldAccountForAllResourceBundleInClassPath() {
        final Path resourceFolder =
                get(getResource("triplestore.properties").getPath()).getParent();

        final List<String> resourceNames = Files.list(resourceFolder)
                .filter(fileName -> fileName.toString().endsWith(".properties"))
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());

        assertThat("ResourceLoader enum should have all the resources ",
                of(Resource.values()).map(Object::toString).map(name -> name + ".properties").toList(), Matchers.containsInAnyOrder(resourceNames.toArray()));

    }

    @Test
    public void shouldGetAvailableResource() throws Exception {

        final Optional<String> useEmbedded = ResourceLoader.get.apply(Resource.triplestore, "useEmbedded");
        assertThat(useEmbedded.isPresent(), Matchers.equalTo(true));

    }

    @Test
    public void shouldReturnEmptyForUnavailableResource() throws Exception {

        final Optional<String> useEmbedded = ResourceLoader.get.apply(Resource.triplestore, "somethineElse");
        assertThat(useEmbedded.isPresent(), Matchers.equalTo(false));

    }
}