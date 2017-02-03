package recipestore.input.graphcreator;

import com.google.common.collect.LinkedListMultimap;
import lombok.*;
import org.apache.jena.ontology.Individual;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * An adjacent list implementation of property graph
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "uri")
@ToString(of = {"uri"})
public class GraphVertex {

    private final String uri;
    private String type;
    private Resource rdfType;
    private final boolean isAnon;
    private final String name;
    private LinkedListMultimap<String, Object> propertyValue = LinkedListMultimap.create();

    public GraphVertex(final Resource resource, final Resource rdfType) {
        this.isAnon = resource.isAnon();
        final String uri = isNullOrEmpty(resource.getURI()) ?
                null : resource.getURI();

        this.uri = (this.isAnon) ? resource.toString() : firstNonNull(uri,
                resource.getLocalName());
        this.name = resource.getLocalName();
        this.rdfType = rdfType;
        this.type = getTypeString(rdfType);
    }

    private String getTypeString(Resource rdfType) {
        return rdfType != null ? rdfType.getLocalName().replaceAll("-", "") : isAnon ? "Container" : "Resource";
    }

    public LinkedListMultimap<String, Object> getPropertyValue() {
        return propertyValue == null ? LinkedListMultimap.create() : propertyValue;

    }

    public GraphVertex(final Resource resource) {
        this(resource, getRDFType(resource));
    }

    public String getType() {
        return firstNonNull(type, getTypeString(rdfType));
    }

    public static Resource getRDFType(Resource resource) {
        try {
            if (resource.canAs(Individual.class)) {
                final ExtendedIterator<Resource> rdfTypes = resource.listProperties(RDF.type)
                        .mapWith(prop -> prop.getObject().asResource());

                if (rdfTypes.hasNext()) {
                    return rdfTypes.next();
                }
            }
        } catch (Exception e) {
            return SKOS.Concept;
        }
        return resource;
    }


}
