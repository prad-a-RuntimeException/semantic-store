package recipestore.input.graphcreator;

import com.github.javafaker.Faker;
import com.google.common.collect.ImmutableMap;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDuration;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.hamcrest.Matchers;
import org.jooq.lambda.Seq;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.jena.ontology.OntModelSpec.OWL_LITE_MEM;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ModelFactory.createOntologyModel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

public class PropertyGraphCreatorTest {


    private static TypeMapper instance;

    static {
        instance = TypeMapper.getInstance();
        XSDDatatype.loadXSDSimpleTypes(instance);
        faker = new Faker();

    }

    private static Faker faker;


    @Test
    public void shouldConvertTriplesToPropertyGraph() {

        final Resource mockResource = createMockResource();

        final PropertyGraphCreator propertyGraphCreator = new PropertyGraphCreator(Seq.of(mockResource));
        final List<PropertyGraph> graphList = propertyGraphCreator.getResources().collect(Collectors.toList());
        assertThat("Should have single entry in the returned graph ", graphList.size(), Matchers.equalTo(1));

        final PropertyGraph propertyGraph = graphList.get(0);
        final GraphVertex recipeVertex = propertyGraph.getRoot();

        assertThat("Recipe vertex should have necessary properties", recipeVertex.getPropertyValue().keySet(),
                hasItems("commentCount", "prepTime", "cookingMethod",
                        "text", "recipeIngredient", "recipeCategory"));

        final List<GraphEdge> graphEdges = propertyGraph.getAdjacentEdges().get(recipeVertex);

        assertThat("Recipe vertex should have two edges", graphEdges.stream()
                        .map(edge -> edge.getName()).collect(Collectors.toSet()),
                hasItems("review", "nutrition"));

    }


    public static Resource createMockResource() {

        final Model base = createDefaultModel().read("http://topbraid.org/schema/schema.ttl", Lang.NTRIPLES.toString());
        final OntModel recipeModel = createOntologyModel(OWL_LITE_MEM, base);


        List<String> recipeDataProperties
                = newArrayList("http://schema.org/commentCount",
                "http://schema.org/prepTime",
                "http://schema.org/cookingMethod",
                "http://schema.org/text",
                "http://schema.org/recipeIngredient",
                "http://schema.org/recipeCategory");

        List<String> recipeReviewProperty
                = newArrayList("http://schema.org/text");

        List<String> recipeNutritionProperty
                = newArrayList("http://schema.org/text");

        Map<String, List<String>> resourceProperties = ImmutableMap.of("http://schema.org/review", recipeReviewProperty, "http://schema.org/nutrition", recipeNutritionProperty);

        List<String> recipeObjectProperties
                = newArrayList("http://schema.org/review",
                "http://schema.org/nutrition");

        final Resource recipeResource = createResource("http://schema.org/Recipe", recipeModel, recipeDataProperties);

        recipeObjectProperties.forEach(objectProperty ->
                recipeResource.addProperty(recipeModel.getProperty(objectProperty), createResource(recipeModel.getProperty(objectProperty).as(ObjectProperty.class).getRange().getURI(), recipeModel, resourceProperties.get(objectProperty))));


        return recipeResource;


    }

    private static Resource createResource(final String uri, OntModel recipeModel, List<String> dataTypeProperties) {
        Resource resource = recipeModel.createResource(uri);
        dataTypeProperties.forEach(property -> {
            final Property ontProperty = recipeModel.getProperty(property);
            resource.addProperty(ontProperty, getValue(ontProperty));
        });
        return resource;
    }

    private static RDFNode getValue(Property ontProperty) {
        final Class javaClass = getJavaClass(ontProperty);

        Map<Class, Supplier<RDFNode>> suppliers = ImmutableMap.<Class, Supplier<RDFNode>>builder()
                .put(XSDDuration.class, () -> ontProperty.getModel().createTypedLiteral(faker.date().past(10, TimeUnit.DAYS)))
                .put(BigInteger.class, () -> ontProperty.getModel().createTypedLiteral(faker.number().randomDigitNotZero()))
                .put(Date.class, () -> ontProperty.getModel().createTypedLiteral(faker.date().past(1000, TimeUnit.DAYS)))
                .put(BigDecimal.class, () -> ontProperty.getModel().createTypedLiteral(faker.number().randomDouble(2, 1, 100)))
                .put(String.class, () -> ontProperty.getModel().createTypedLiteral(faker.food().ingredient()))
                .put(Boolean.class, () -> ontProperty.getModel().createTypedLiteral(faker.bool().bool()))
                .build();

        final Optional<RDFNode> literalVal = newArrayList(suppliers.keySet())
                .stream()
                .filter(clz -> clz.isAssignableFrom(javaClass))
                .map(clz -> suppliers.get(clz).get()).findFirst();
        if (literalVal.isPresent()) {
            return literalVal.get();
        }
        throw new RuntimeException("Failed extracting data type from ontProperty");

    }

    private static Class getJavaClass(Property ontProperty) {
        final String uri = ontProperty.as(DatatypeProperty.class).getRange().getURI();
        final RDFDatatype typeByName = instance.getTypeByName(uri);
        final Class javaClass = typeByName.getJavaClass();
        return javaClass;
    }

}