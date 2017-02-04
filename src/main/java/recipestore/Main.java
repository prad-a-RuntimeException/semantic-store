package recipestore;

import dagger.Component;
import recipestore.input.DaggerInputComponent;
import recipestore.input.InputModule;
import recipestore.input.RecipeApi;

class Main {
    @Component(modules = InputModule.class)
    public interface InputComponent {
        RecipeApi getRecipeLoader();
    }

    public static void recipeDataLoader() {
        final recipestore.input.InputComponent inputComponent = DaggerInputComponent.builder()
                .build();

        inputComponent.getRecipeLoader().loadRecipe(false);
    }


}