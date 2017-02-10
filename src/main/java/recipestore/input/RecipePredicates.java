package recipestore.input;

import java.util.List;
import java.util.function.Predicate;

import static com.google.common.collect.Lists.newArrayList;

public class RecipePredicates {

    public static final List<String> RECIPE_URL_FILTER =
            newArrayList(
                    "epicurious.com",
                    "nytimes",
                    "allrecipes.com",
                    "allrecipes.co.uk"
            );


    public static final Predicate<String> filterByUrl = (url) ->
            RECIPE_URL_FILTER.stream().anyMatch(pattern -> url.toLowerCase().contains(pattern));


}
