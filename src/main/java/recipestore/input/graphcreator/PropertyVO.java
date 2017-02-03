package recipestore.input.graphcreator;

import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
@ToString
public class PropertyVO {

    public static final Logger LOGGER = LoggerFactory.getLogger(PropertyVO.class);

    public enum PropertyFacet {
        hasClaim, hasComponent, hasOrigin, hasPhysicalProperty, isType, Category, Subcategory, Uri, Synonyms, Unknown, Type
    }

    private final String parentProperty;
    private PropertyFacet propertyFacet;
    private final String property;
    private final String value;

    private PropertyVO(String parentProperty, String property, String value) {
        this.parentProperty = parentProperty;
        this.property = property;
        this.value = value.indexOf("-") > 0 ? value.split("-")[1] : value;
        try {
            this.propertyFacet = PropertyFacet.valueOf(parentProperty);
        } catch (IllegalArgumentException e) {
            LOGGER.trace("Ignoring attributes ", e);
        }
    }

    public String getValue() {
        if (!Strings.isNullOrEmpty(this.value)) {
            return Seq.of(StringUtils.splitByCharacterTypeCamelCase(this.value))
                    .filter(StringUtils::isAlpha).collect(Collectors.joining(" "));

        }
        return this.value;
    }

    public static PropertyVO of(String parentPreperty, String property, String value) {
        return new PropertyVO(parentPreperty, property, value);
    }


}
