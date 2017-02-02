package recipestore.input;

import dagger.Component;

@Component(modules = InputModule.class)
public interface InputComponent {

    RecipeApi getRecipeLoader();
}
