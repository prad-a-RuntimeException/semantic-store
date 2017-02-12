package recipestore.db.triplestore.rdfparsers;

import com.google.common.collect.Lists;
import org.apache.jena.riot.tokens.TokenType;
import org.apache.jena.riot.tokens.Tokenizer;
import org.hamcrest.Matchers;
import org.jooq.lambda.Seq;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;

public class ProactiveTokenzierTest {



    @Test
    public void shouldTokenizeBadURI() {
        final List<TokenType> expected = Lists.newArrayList(TokenType.BNODE, TokenType.IRI, TokenType.LITERAL_LANG, TokenType.IRI
                , TokenType.DOT);

        final List<String> expectedValue = Lists.newArrayList("node2ea0be87cb5aa410715f477d8126cd8f",
                "http://schema.org/NutritionInformation/sodiumContent",
                "Sodium 480mg",
                "http://relish.com/recipes/creamy-braising-greens-soup/", null);

        final String quadWithBadURI =
                "_:node2ea0be87cb5aa410715f477d8126cd8f <http://schema.org/NutritionInformation/\\\"sodiumContent\\\"> \"Sodium 480mg\"@en-us <http://relish.com/recipes/creamy-braising-greens-soup/> .";
        final Tokenizer tokenizer = ProactiveTokenzier.create(quadWithBadURI);
        final List<AbstractMap.SimpleEntry<TokenType, String>> actualValues = Seq.seq(tokenizer).map(token -> new AbstractMap.SimpleEntry<>(token.getType(), token.getImage()))
                .toList();


        assertThat("Contains TokenTypes in the correct order", actualValues.stream().map(val -> val.getKey()).collect(Collectors.toList()), Matchers.equalTo(expected));
        assertThat("Contains values in the correct order", actualValues.stream().map(val -> val.getValue()).collect(Collectors.toList()), Matchers.equalTo(expectedValue));


    }

}