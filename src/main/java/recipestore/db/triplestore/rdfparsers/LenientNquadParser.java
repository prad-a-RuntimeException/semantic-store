package recipestore.db.triplestore.rdfparsers;

import org.apache.jena.graph.Node;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.lang.LangNTuple;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.tokens.Token;
import org.apache.jena.riot.tokens.TokenType;
import org.apache.jena.riot.tokens.Tokenizer;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Since the data we typically get from the internet is faulty, we need
 * a more lenient parser, that would handle malformed quads more gracefully,
 * without bailing out
 * <p>
 *
 * @see: LangNQuads.java. The original class is not designed to be extendable.
 */
public class LenientNquadParser extends LangNTuple<Quad> {

    private Node currentGraph = null;
    public static final String MALFORMED_NQUAD_LANG = "malformed_nquad";
    public static final Logger LOGGER = LoggerFactory.getLogger(LenientNquadParser.class);
    public static final Lang LANG =
            createLang();

    private static Lang createLang() {
        final Lang originalNquadLang = RDFLanguages.NQUADS;
        return LangBuilder.create(MALFORMED_NQUAD_LANG, Lang.NQUADS.toString())
                .contentType(originalNquadLang.getContentType().getContentType())
                .build();
    }

    public LenientNquadParser(Tokenizer tokens, ParserProfile profile, StreamRDF dest) {
        super(tokens, profile, dest);
    }

    @Override
    public Lang getLang() {
        if (!RDFLanguages.isRegistered(LANG)) {
            RDFLanguages.register(LANG);
        }
        return RDFLanguages.nameToLang(MALFORMED_NQUAD_LANG);
    }

    /**
     * Method to parse the whole stream of triples, sending each to the sink
     */
    @Override
    protected final void runParser() {
        while (hasNext()) {
            Quad x = null;
            try {
                x = parseOne();
            } catch (Exception e) {
                LOGGER.warn("NQuad parsing failed {}", e.getMessage());

            }

            if (x != null)
                dest.quad(x);
        }
    }

    @Override
    protected final Quad parseOne() {
        Token sToken = nextToken();
        if (sToken.getType() == TokenType.EOF)
            exception(sToken, "Premature end of file: %s", sToken);

        Token pToken = nextToken();
        if (pToken.getType() == TokenType.EOF)
            exception(pToken, "Premature end of file: %s", pToken);

        Token oToken = nextToken();
        if (oToken.getType() == TokenType.EOF)
            exception(oToken, "Premature end of file: %s", oToken);

        Token xToken = nextToken();    // Maybe DOT
        if (xToken.getType() == TokenType.EOF)
            exception(xToken, "Premature end of file: Quad not terminated by DOT: %s", xToken);

        // Process graph node first, before S,P,O
        // to set bnode label scope (if not global)
        Node c = null;

        if (xToken.getType() != TokenType.DOT) {
            // Allow bNodes for graph names.
            checkIRIOrBNode(xToken);
            // Allow only IRIs
            //checkIRI(xToken) ;
            c = tokenAsNode(xToken);
            xToken = nextToken();
            currentGraph = c;
        } else {
            c = Quad.defaultGraphNodeGenerated;
            currentGraph = null;
        }

        // createQuad may also check but these checks are cheap and do form syntax errors.
        checkIRIOrBNode(sToken);
        checkIRI(pToken);
        checkRDFTerm(oToken);
        // xToken already checked.

        Node s = tokenAsNode(sToken);
        Node p = tokenAsNode(pToken);
        Node o = tokenAsNode(oToken);

        // Check end of tuple.

        if (xToken.getType() != TokenType.DOT)
            exception(xToken, "Quad not terminated by DOT: %s", xToken);

        return profile.createQuad(c, s, p, o, sToken.getLine(), sToken.getColumn());
    }

    @Override
    protected final Node tokenAsNode(Token token) {
        return profile.create(currentGraph, token);
    }
}
