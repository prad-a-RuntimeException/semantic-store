package recipestore;

import com.google.common.base.Joiner;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipestore.input.InputModule;
import recipestore.input.RecipeApi;

import java.util.stream.Stream;

class Main {

    public static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static Injector inputModule = Guice.createInjector(new InputModule());

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

    public static void loadRecipeData() {
        inputModule.getInstance(RecipeApi.class).loadRecipe(true);
    }

    public static void readRecipeData() {
        final RecipeApi instance = inputModule.getInstance(RecipeApi.class);
        final Stream<Resource> recipeData = instance
                .getRecipeData();

    }


}