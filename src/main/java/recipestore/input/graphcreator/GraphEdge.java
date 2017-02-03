package recipestore.input.graphcreator;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"uri", "inVertex"})
@ToString(of = {"uri", "name"})
public class GraphEdge {
    private final String uri;
    private final String name;
    private final GraphVertex inVertex;
}
