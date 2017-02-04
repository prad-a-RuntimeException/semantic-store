package recipestore.nlp.corpus;

import com.google.common.io.Resources;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.item.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.tdb.base.file.FileException;
import org.jooq.lambda.Seq;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

public class WordnetApi {


    private IDictionary dict;

    @Inject
    public WordnetApi() {
        try {
            this.dict = getiDictionary();
        } catch (FileException e) {
            throw new RuntimeException("Cannot find wordnet file ", e);
        }
    }


    public Seq<WordnetResource> getWordnetFoodHierarchy() {
        return getHierarchy().map(this::decorateWithSynonymy);
    }


    private Seq<IWordID> getHierarchy() {
        return Seq.seq(dict.getIndexWordIterator(POS.NOUN))
                .flatMap(iIndexWord -> iIndexWord.getWordIDs().stream())
                .filter(word -> dict.getSynset(word.getSynsetID()).getLexicalFile().getName().contains("food"));

    }

    private IDictionary getiDictionary() {
        try {
            final URL resource = Resources.getResource("wordnet.fn");
            final InputStream inputStream = resource.openStream();
            final File file = File.createTempFile("wordnet", "tmp");
            FileUtils.copyInputStreamToFile(inputStream, file);
            IDictionary dict = new RAMDictionary(file);
            dict.open();
            return dict;
        } catch (IOException e) {
            throw new RuntimeException("Cannot open wordnet directory ", e);
        }
    }


    private WordnetResource decorateWithSynonymy(final IWordID wordId) {
        final IWord word = dict.getWord(wordId);
        ISynset synset = word.getSynset();
        final Stream<String> words = synset.getWords().stream()
                .map(s -> s.getLemma())
                .map(synonym -> StringUtils.remove(synonym, "s'"));
        final Stream<String> hyponyms = getPointers(dict, synset, Pointer.HYPONYM);
        final Stream<String> hypernyms = getPointers(dict, synset, Pointer.HYPERNYM);
        return new WordnetResource(StringUtils.remove(word.getLemma(), "s'"), words, hyponyms, hypernyms);
    }

    private Stream<String> getPointers(IDictionary dict, ISynset synset, Pointer pointer) {
        List<ISynsetID> pointers = synset.getRelatedSynsets(pointer);
        return pointers
                .stream()
                .flatMap(id -> dict.getSynset(id).getWords().stream())
                .map(word -> word.getLemma());

    }

}
