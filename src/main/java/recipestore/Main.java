package recipestore;

import com.codahale.metrics.Counter;
import dagger.Component;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipestore.input.DaggerInputComponent;
import recipestore.input.InputModule;
import recipestore.input.RecipeApi;
import recipestore.metrics.MetricsFactory;

import java.util.stream.Stream;

class Main {

    public static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        recipeDataReader();
    }

    @Component(modules = InputModule.class)
    public interface InputComponent {
        RecipeApi getRecipeLoader();
    }

    public static void recipeDataLoader() {
        final recipestore.input.InputComponent inputComponent = DaggerInputComponent.builder()
                .build();

        inputComponent.getRecipeLoader().loadRecipe(true);
    }

    public static void recipeDataReader() {
        final recipestore.input.InputComponent inputComponent = DaggerInputComponent.builder()
                .build();


        final Stream<Resource> recipeData = inputComponent.getRecipeLoader()
                .getRecipeData();
        final Counter recipeReaderCounter = MetricsFactory.getMetricFactory().initializeCounter("RecipeReaderCounter");
        recipeData
                .forEach(resource -> {
                    try {
                        recipeReaderCounter.inc();
                    } catch (Exception e) {
                    }
                });
    }


}