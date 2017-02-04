package recipestore.nlp.corpus;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;

public class WordnetApiTest {

    @Test
    public void shouldLoadFoodInformationFromWordnet() {

        final WordnetApi wordnetApi = new WordnetApi();

        final List<WordnetResource> wordnetFoodHierarchy = wordnetApi.getWordnetFoodHierarchy().toList();

        assertThat("Should have loaded food data from wordnet", wordnetFoodHierarchy,
                Matchers.not(Matchers.emptyIterable()));

        final Optional<WordnetResource> onion = wordnetFoodHierarchy
                .stream()
                .filter(wordnetResource -> wordnetResource.getWord().equals("onion"))
                .findFirst();

        assertThat("Should find onion ", onion.isPresent(), Matchers.equalTo(true));
        assertThat("Onion should be of type vegetable ", onion.get().getHypernyms().collect(Collectors.toList()),
                Matchers.hasItem("vegetable"));

        assertThat("Onion should have the following sub type ", onion.get().getHyponyms().collect(Collectors.toList()),
                Matchers.hasItems("Bermuda_onion",
                        "green_onion",
                        "spring_onion",
                        "scallion",
                        "Spanish_onion",
                        "shallot"));


    }

}