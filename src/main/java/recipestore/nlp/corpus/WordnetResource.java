package recipestore.nlp.corpus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jooq.lambda.Seq;

import java.util.stream.Stream;

@RequiredArgsConstructor
@Getter
@ToString(of = "word")
public class WordnetResource {
    private final String word;
    private final Stream<String> synonyms;
    private final Stream<String> hyponyms;
    private final Stream<String> hypernyms;

    public Stream<String> getTokens() {
        return Seq.concat(this.synonyms, this.hypernyms, this.hyponyms, Seq.of(word));
    }


}