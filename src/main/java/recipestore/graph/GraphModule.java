package recipestore.graph;

import dagger.Module;
import dagger.Provides;

import java.util.ResourceBundle;

@Module
public class GraphModule {

    public static final String GRAPHFRAME_DIR = "graphframe_dir";

    @Provides
    public static String graphDirectory() {
        final ResourceBundle bundle = ResourceBundle.getBundle("graph");
        assert bundle.containsKey(GRAPHFRAME_DIR);
        return String.format("file://%s", bundle
                .getString(GRAPHFRAME_DIR));
    }


}
