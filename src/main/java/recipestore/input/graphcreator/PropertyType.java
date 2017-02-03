package recipestore.input.graphcreator;

import com.google.common.collect.Lists;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
@ToString()
@EqualsAndHashCode(of = {"domain", "propertyName"})
public class PropertyType {
    private final String domain;
    private final String propertyName;
    private final VertexProperty.Cardinality cardinality;
    private final Class dataType;

    public final List<Class> Priority_Order =
            Lists.newArrayList(String.class, Double.class, Integer.class, Boolean.class, Date.class);

    public List<PropertyType> merge(final PropertyType that) {
        if (that.equals(this)) {
            VertexProperty.Cardinality cardinality;
            if (this.cardinality == that.cardinality) {
                cardinality = this.cardinality;
            } else {
                cardinality = VertexProperty.Cardinality.list;
            }

            final ArrayList<Class> classes = Lists.newArrayList(that.getDataType(), this.dataType);

            final Optional<Class> mostInclusiveClassOptional = Priority_Order
                    .stream()
                    .filter(thisClass -> classes.stream().allMatch(clz -> thisClass.isAssignableFrom(clz)))
                    .findFirst();

            final Class aClass = mostInclusiveClassOptional.isPresent() ? mostInclusiveClassOptional.get() : String.class;

            return Lists.newArrayList(new PropertyType(domain, propertyName, cardinality, aClass));
        }
        //Merge impossible so return list
        return Lists.newArrayList(this, that);
    }

}