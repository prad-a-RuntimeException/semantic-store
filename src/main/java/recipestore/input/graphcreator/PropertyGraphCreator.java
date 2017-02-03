package recipestore.input.graphcreator;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import lombok.Getter;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.jena.vocabulary.OWL2.ObjectProperty;
import static org.jooq.lambda.Seq.*;

public class PropertyGraphCreator {

    public static final Logger LOGGER = LoggerFactory.getLogger(PropertyGraphCreator.class);

    private final List<PropertyGraph> graphList;
    @Getter
    private final Stream<PropertyGraph> resources;

    public Stream<PropertyGraph> getResources() {
        return resources;
    }

    public PropertyGraphCreator(final Stream<Resource> resources) {
        Tuple2<Seq<Tuple2<Resource, Long>>, Seq<Tuple2<Resource, Long>>> partitionedResourceStream =
                partition(zipWithIndex(resources), (tup) -> tup.v2() < 10);
        this.graphList = partitionedResourceStream.v1().map(resource -> resource.v1())
                .map(resource -> extractInstance(resource)).collect(Collectors.toList());
        this.resources = Seq.concat(partitionedResourceStream.v2().map(resource -> resource.v1())
                .map(resource -> extractInstance(resource)), graphList.stream());
    }


    private GraphVertex getGraphVertex(Resource resource) {
        Resource type = GraphVertex.getRDFType(resource);
        return new GraphVertex(resource, type);
    }

    private PropertyGraph extractInstance(Resource resource) {
        GraphVertex resourceVertex = getGraphVertex(resource);
        PropertyGraph graph = new PropertyGraph(resourceVertex);
        graph.addGraphVertex(resourceVertex);
        resourceVertex.setPropertyValue(extractDataProperties(resource));

        Seq.seq(resource.listProperties())
                .filter(stmt -> stmt.getObject().canAs(Resource.class))
                .forEach(child -> extractInstance(child, resourceVertex, graph));

        return graph;

    }

    private PropertyGraph extractInstance(Statement statement, GraphVertex parentVertex, final PropertyGraph graph) {
        final Resource resource = statement.getObject().asResource();
        GraphVertex resourceVertex = getGraphVertex(resource);
        graph.addEdges(parentVertex, new GraphEdge(statement.getPredicate().getURI(), statement.getPredicate().getLocalName(), resourceVertex));
        graph.addGraphVertex(resourceVertex);
        resourceVertex.setPropertyValue(extractDataProperties(resource));

        Seq.seq(resource.listProperties())
                .filter(stmt -> stmt.getPredicate().canAs(ObjectProperty.getClass()))
                .filter(stmt -> stmt.getObject().canAs(Resource.class))
                .forEach(child -> extractInstance(child, resourceVertex, graph));


        return graph;

    }


    private static LinkedListMultimap<String, Object> extractDataProperties(Resource resource) {
        Map<String, Object> propertyValues = Maps.newHashMap();
        seq(resource.listProperties())
                .filter(prop -> prop.getObject().canAs(Literal.class))
                .forEach(prop -> propertyValues.put(prop.getPredicate().getLocalName(),
                        prop.getObject().asLiteral().getValue()));

        LinkedListMultimap<String, Object> propertyMap = LinkedListMultimap.create();

        propertyValues.entrySet()
                .forEach(entry -> {
                    if (entry.getValue() instanceof Collection)
                        propertyMap.putAll(entry.getKey(), Collection.class.cast(entry.getValue()));
                    else
                        propertyMap.put(entry.getKey(), entry.getValue());
                });

        propertyMap.put("uri", resource.getURI());
        return propertyMap;
    }


    public Map<Tuple2<String, String>, PropertyType> getInferPropertyTypes() {
        return inferPropertyTypes(graphList);
    }


    private static Class<?> getSupportedClass(Object firstValue) {

        final List<Class> ORDER = newArrayList(Date.class, Boolean.class, Double.class, Integer.class, String.class);

        final Optional<Class> first = ORDER.stream()
                .filter(thisClass -> thisClass.isAssignableFrom(firstValue.getClass()))
                .findFirst();
        return first.isPresent() ? first.get() : String.class;

    }

    private Map<Tuple2<String, String>, PropertyType> inferPropertyTypes(List<PropertyGraph> propertyGraphStream) {


        final ImmutableMap.Builder<Tuple2<String, String>, PropertyType> mapBuilder = ImmutableMap.builder();
        propertyGraphStream
                .stream()
                .flatMap(graph -> graph.getGraphVertices()
                        .stream()
                        .filter(graphVertex -> graphVertex.getPropertyValue() != null)
                        .map(graphVertex ->
                                new Tuple2<>(graphVertex, graphVertex.getPropertyValue())))
                .flatMap(tup -> getPropertyTypes.apply(tup))
                .collect(Collectors.groupingBy(val -> val))
                .entrySet()
                .stream()
                .map(entry -> {
                    final Optional<PropertyType> reduce = entry.getValue().stream()
                            .reduce((mem, curr) -> mem.merge(curr).get(0));
                    return reduce.get();
                }).forEach(propertyType -> mapBuilder.put(new Tuple2<>(propertyType.getDomain(), propertyType.getPropertyName()), propertyType));
        return mapBuilder.build();
    }

    private Function<Tuple2<GraphVertex, LinkedListMultimap<String, Object>>, Stream<PropertyType>> getPropertyTypes =
            (tuple) -> {
                final String domain = tuple.v1().getType();
                return tuple.v2().keys()
                        .stream()
                        .map(key -> getPropertyType(domain, key, tuple.v2().get(key)));
            };

    private PropertyType getPropertyType(final String domain, final String propertyKey, final Collection<Object> values) {
        VertexProperty.Cardinality cardinality = VertexProperty.Cardinality.single;
        Class supportedClass = String.class;
        if (values.size() > 1) {
            cardinality = VertexProperty.Cardinality.list;
        } else if (values.size() == 1) {
            final Object firstValue = values.iterator().next();
            supportedClass = getSupportedClass(firstValue);
            cardinality = VertexProperty.Cardinality.list;
        }
        return new PropertyType(domain, propertyKey, cardinality, supportedClass);
    }
}
