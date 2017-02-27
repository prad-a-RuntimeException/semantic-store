package recipestore;

import com.google.inject.Guice;
import com.google.inject.Injector;
import recipestore.input.InputModule;
import recipestore.input.RecipeApi;

class TripleStoreEntryPoint {

    public static Injector inputModule = Guice.createInjector(new InputModule());

    public static void main(String[] args) {
        loadRecipeData();
    }

    public static void loadRecipeData() {
        inputModule.getInstance(RecipeApi.class).loadRecipe(true);
    }


}