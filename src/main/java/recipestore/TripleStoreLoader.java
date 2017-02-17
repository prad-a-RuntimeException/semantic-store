package recipestore;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipestore.input.InputModule;
import recipestore.input.RecipeApi;

class TripleStoreLoader {

    public static final Logger LOGGER = LoggerFactory.getLogger(TripleStoreLoader.class);
    public static Injector inputModule = Guice.createInjector(new InputModule());


    public static void main(String[] args) {
        loadRecipeData();

    }

    public static void loadRecipeData() {
        inputModule.getInstance(RecipeApi.class).loadRecipe(true);
    }


}