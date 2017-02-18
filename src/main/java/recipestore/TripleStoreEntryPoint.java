package recipestore;

import com.google.common.base.Joiner;
import com.google.inject.Guice;
import com.google.inject.Injector;
import recipestore.input.InputModule;
import recipestore.input.RecipeApi;

class TripleStoreEntryPoint {

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
        instance.getRecipeData()
                .forEach(r -> r.listProperties().toList());
    }


}