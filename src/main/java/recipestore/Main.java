package recipestore;

import com.codahale.metrics.Counter;
import com.google.common.base.Joiner;
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

    public enum Command {
        LoadRecipeData, ReadRecipeData
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new RuntimeException(String.format("Command argument is mandatory and should be one of %s ",
                    Joiner.on(",").join(Command.values())));
        }
        final Command command;
        try {
            final String commandString = args[0];
            command = Command.valueOf(commandString);
            switch (command) {
                case LoadRecipeData:
                    loadRecipeData();
                    break;
                case ReadRecipeData:
                    readRecipeData();
                    break;
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(String.format("Command argument is not valid and should be one of %s ",
                    Joiner.on(",").join(Command.values())));
        }

    }

    @Component(modules = InputModule.class)
    public interface InputComponent {
        RecipeApi getRecipeLoader();
    }

    public static void loadRecipeData() {
        final recipestore.input.InputComponent inputComponent = DaggerInputComponent.builder()
                .build();

        inputComponent.getRecipeLoader().loadRecipe(true);
    }

    public static void readRecipeData() {
        final recipestore.input.InputComponent inputComponent = DaggerInputComponent.builder()
                .build();


        final Stream<Resource> recipeData = inputComponent.getRecipeLoader()
                .getRecipeData();
        final Counter recipeReaderCounter = MetricsFactory.getMetricFactory().initializeCounter("RecipeReaderCounter");
        recipeData
                .peek(resource -> System.out.println(resource))
                .forEach(resource -> {
                    try {
                        recipeReaderCounter.inc();
                    } catch (Exception e) {
                        LOGGER.warn("Failed reading recipe data", e);
                    }
                });
    }


}