package recipestore;

import lombok.RequiredArgsConstructor;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class ResourceLoader {
    final static Map<Resource, ResourceBundle> resourceCache;

    static {
        resourceCache = Seq.of(Resource.values())
                .map(resource -> new Tuple2<>(resource, ResourceBundle.getBundle(resource.resourceName)))
                .collect(Collectors.toMap(tuple -> tuple.v1, tuple -> tuple.v2));
    }

    @RequiredArgsConstructor
    public enum Resource {
        triplestore("triplestore"), graph("graph"), lucene("lucene");
        private final String resourceName;

        @Override
        public String toString() {
            return resourceName;
        }
    }

    public static BiFunction<Resource, String, Optional<String>> get = (resource, key) ->
            ofNullable(resourceCache.get(resource).containsKey(key) ?
                    resourceCache.get(resource).getString(key)
                    : null);

}
