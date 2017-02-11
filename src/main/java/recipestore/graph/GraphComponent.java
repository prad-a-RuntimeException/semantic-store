package recipestore.graph;

import dagger.Component;

import javax.inject.Inject;

@Component(modules = GraphModule.class)
public interface GraphComponent {

    class GraphDirectory {
        private final String graphDirectory;

        @Inject
        public GraphDirectory(String graphDirectory) {
            this.graphDirectory = graphDirectory;
        }

        public String get() {
            return graphDirectory;
        }
    }

    GraphDirectory getGraphDirectory();

}
