package recipestore.graph;

import dagger.Module;
import dagger.Provides;

@Module
public class GraphModule {

    @Provides
    public static String graphDirectory() {
        return String.format("hdfs://%s", "recipe_graph");
    }


}
