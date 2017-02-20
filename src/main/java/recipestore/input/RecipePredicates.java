package recipestore.input;

import java.util.List;
import java.util.function.Predicate;

import static com.google.common.collect.Lists.newArrayList;

public class RecipePredicates {

    public static final List<String> RECIPE_URL_FILTER =
            newArrayList(
                    "allrecipes.com",
                    "foodnetwork.com",
                    "thekitchn.com",
                    "yummly.com",
                    "chow.com",
                    "epicurious.com",
                    "simplyrecipes.com",
                    "food.com",
                    "bettycrocker.com",
                    "cookinglight.com"

            );


    public static final Predicate<String> filterByUrl = (url) ->
            RECIPE_URL_FILTER.stream().anyMatch(pattern -> url.toLowerCase().contains(pattern));


}
