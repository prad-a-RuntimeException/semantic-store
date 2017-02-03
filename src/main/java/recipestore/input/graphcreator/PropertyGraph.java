package recipestore.input.graphcreator;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class PropertyGraph {

    private final GraphVertex root;
    private Set<GraphVertex> graphVertices = Sets.newHashSet();
    private LinkedListMultimap<GraphVertex, GraphEdge> adjacentEdges = LinkedListMultimap.create();

    public PropertyGraph(GraphVertex root) {
        this.root = root;
        this.graphVertices.add(root);
    }

    public void addGraphVertex(final GraphVertex graphVertex) {
        this.graphVertices.add(graphVertex);
    }

    public void addEdges(final GraphVertex outVertex, GraphEdge edge) {
        this.graphVertices.add(edge.getInVertex());
        this.adjacentEdges.put(outVertex, edge);
    }


}
