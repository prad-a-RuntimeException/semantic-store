/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package recipestore.db.triplestore.rdfparsers;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.*;
import org.apache.jena.riot.lang.LangRIOT;
import org.apache.jena.riot.system.*;
import org.apache.jena.riot.tokens.Tokenizer;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.system.JenaSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.util.Objects;

/**
 * <p>General purpose reader framework for RDF (triples and quads) syntaxes.</p>
 * <ul>
 * <li>HTTP Content negotiation</li>
 * <li>File type hint by the extension</li>
 * <li>Application language hint</li>
 * </ul>
 * <p>
 * It also provides a way to lookup names in different
 * locations and to remap URIs to other URIs.
 * </p>
 * <p>
 * Extensible - a new syntax can be added to the framework.
 * </p>
 * <p>Operations fall into the following categories:</p>
 * <ul>
 * <li>{@code read}    -- Read data from a location into a Model/Dataset etc</li>
 * <li>{@code loadXXX} -- Read data and return an in-memory object holding the data.</li>
 * <li>{@code parse}   -- Read data and send to an {@link StreamRDF}</li>
 * <li>{@code open}    -- Open a typed input stream to the location, using any alternative locations</li>
 * <li>{@code write}   -- Write Model/Dataset etc</li>
 * <li>{@code create}  -- Create a reader or writer explicitly</li>
 * </ul>
 */

public class CustomRDFDataMgr extends RDFDataMgr {
    static {
        JenaSystem.init();
    }

    static Logger log = LoggerFactory.getLogger(CustomRDFDataMgr.class);
    private static String riotBase = "http://jena.apache.org/riot/";

    private static class MalformedNquadReader implements ReaderRIOT {
        private final Lang lang;
        private ErrorHandler errorHandler;
        private ParserProfile parserProfile = null;

        MalformedNquadReader() {
            this.lang = LenientNquadParser.LANG;
            errorHandler = ErrorHandlerFactory.getDefaultErrorHandler();
        }


        @Override
        public void read(InputStream in, String baseURI, ContentType ct, StreamRDF output, Context context) {
            @SuppressWarnings("deprecation")
            LangRIOT parser = createParser(in, output);
            parser.parse();
        }


        @Override
        public void read(Reader in, String baseURI, ContentType ct, StreamRDF output, Context context) {
            @SuppressWarnings("deprecation")
            LangRIOT parser = createParser(new ReaderInputStream(in), output);
            parser.getProfile().setHandler(errorHandler);
            parser.parse();
        }

        @Override
        public ErrorHandler getErrorHandler() {
            return errorHandler;
        }

        @Override
        public void setErrorHandler(ErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
        }

        @Override
        public ParserProfile getParserProfile() {
            return parserProfile;
        }

        @Override
        public void setParserProfile(ParserProfile parserProfile) {
            this.parserProfile = parserProfile;
            this.errorHandler = parserProfile.getHandler();
        }
    }

    /**
     * Read RDF data.
     *
     * @param sink Destination for the RDF read.
     * @param in   Bytes to read.
     * @param lang Syntax for the stream.
     */
    public static void parse(StreamRDF sink, InputStream in, Lang lang) {
        process(sink, new TypedInputStream(in), null, lang, null);
    }

    private static ReaderRIOT getReader(ContentType ct) {
        Lang lang = RDFLanguages.contentTypeToLang(ct);
        if (lang == null)
            return null;
        ReaderRIOTFactory r = RDFParserRegistry.getFactory(lang);
        if (r == null)
            return null;
        return r.create(lang);
    }


    public static LangRIOT createParser(InputStream input, StreamRDF dest) {

        Tokenizer tokenizer = LenientTokenizer.create(input);
        ParserProfile profile = RiotLib.profile(RDFLanguages.NQUADS, null);
        return new LenientNquadParser(tokenizer, profile, dest);
    }


    // -----
    // Readers are algorithms and must be stateless (or they must create a per
    // run instance of something) because they may be called concurrency from
    // different threads. The Context Reader object gives the per-run
    // configuration.

    private static void process(StreamRDF destination, TypedInputStream in, String baseUri, Lang lang, Context context) {
        Objects.requireNonNull(in, "TypedInputStream is null");
        ReaderRIOT reader = new MalformedNquadReader();
        reader.read(in, baseUri, null, destination, context);
    }


}
