package recipestore.db.triplestore;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import recipestore.ResourceLoader;

public class CommonModule extends AbstractModule {

    String GRAPHFRAME_DIR = "graphframe_dir";

    protected final String graphDir = String.format("file://%s", ResourceLoader.get.apply(ResourceLoader.Resource.graph,
            GRAPHFRAME_DIR).get());

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("graphDirectory"))
                .toInstance(graphDir);
    }
}
