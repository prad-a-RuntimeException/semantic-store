package recipestore;

import dagger.Component;
import org.apache.jena.rdf.model.Model;
import recipestore.input.DaggerInputComponent;
import recipestore.input.InputModule;
import recipestore.input.RecipeApi;

public class Main {
    @Component(modules = InputModule.class)
    public interface InputComponent {
        RecipeApi getRecipeLoader();
    }

    public static void main(String[] args) {
    }

    public static void recipeDataLoader() {
        final recipestore.input.InputComponent inputComponent = DaggerInputComponent.builder()
                .build();

        inputComponent.getRecipeLoader().loadRecipe(false);
    }

    public static Model getRecipeModel() {
        final recipestore.input.InputComponent inputComponent = DaggerInputComponent.builder()
                .build();

        return inputComponent.getRecipeLoader().get();
    }

}