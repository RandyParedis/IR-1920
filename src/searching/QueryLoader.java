package searching;

import helper.Helper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.spell.SuggestMode;
import org.apache.lucene.store.Directory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * The {@code QueryLoader} class is a class that is able to convert a string to
 * a {@code Query} that can be inputted into the {@link SearchEngine}. It is
 * also able to load in a file and create a list of such {@code Query} objects.
 */
public class QueryLoader {
    private Directory index;
    private IndexReader reader;
    private Analyzer analyzer;

    final private String[] FIELDS = {"title", "body", "answers"};

    /**
     * {@code QueryLoader} constructor.
     * All arguments will be used in generating the suggestions based on the index.
     * @param index     The {@code Directory} index to read from.
     * @param reader    The {@code IndexReader} for the index.
     * @param analyzer  The {@code Analyzer} that needs to be used.
     */
    public QueryLoader(Directory index, IndexReader reader, Analyzer analyzer) {
        this.index = index;
        this.reader = reader;
        this.analyzer = analyzer;
    }

    /**
     * Get a {@code Query} object from a string.
     * @param old           The old string to start from.
     * @param suggestions   The number of suggestions that need to be used in the
     *                      transformations
     * @return  A {@code Query} object, representative of the input string. This object
     *          can immediately be inputted into the {@link SearchEngine}.
     * @see QueryLoader#queryTransform
     */
    public Query getQuery(String old, int suggestions) throws IOException, ParseException {
        String querystr = queryTransform(old, suggestions);
        MultiFieldQueryParser mfqp = new MultiFieldQueryParser(FIELDS, analyzer);
        mfqp.setDefaultOperator(QueryParser.Operator.OR);
        return mfqp.parse(querystr);
    }

    /**
     * Gets a set of {@code Query} objects from a given file. This file contains a list of
     * queries that need to be transformed as described by {@link QueryLoader#queryTransform}.
     * @param filename      The name of the file to load.
     * @param suggestions   The amount of suggestions to use for each term in each query.
     * @return  A {@code List<Query>} containing all queries in this file. They can be looped over
     *          and inputted in the {@link SearchEngine} as such.
     * @see QueryLoader#getQuery
     */
    public List<Query> getQueries(String filename, int suggestions) throws IOException, ParseException {
        List<Query> queries = new ArrayList<>();
        Scanner sc = new Scanner(new File(filename));
        while(sc.hasNextLine()) {
            String line = sc.nextLine();
            queries.add(getQuery(line, suggestions));
        }
        return queries;
    }

    /**
     * Transforms a query string into another query string via stripping whitespace,
     * non-alphanumeric characters and transforming to lower case. On top of that, all
     * terms within an original query are joined in a big OR-gate with the suggestions
     * Lucene provides (in case of typos).
     * @param old       The old string to start from.
     * @param sugs      The amount of suggestions to use.
     * @return  The new string.
     */
    private String queryTransform(String old, int sugs)
            throws IOException {
        String[] words = Helper.transform(old).split(" ");
        List<String> ors = new ArrayList<>();
        for(String word : words) {
            if(word.isEmpty()) { continue; }
            List<String> suggestions = new ArrayList<>(Arrays.asList(suggest(word, sugs)));
            if(!suggestions.contains(word)) {
                suggestions.add(word);
            }
            ors.add("(" + String.join(" OR ", suggestions) + ")");
        }
        return String.join(" AND ", ors);
    }

    /**
     * Suggest a list of replacements for the given word. Useful for typos and incomplete matches.
     * @param word  The word to create a suggestion for.
     * @param sugs  The amount of suggestions to create.
     * @return  A {@code String[]} containg all the suggestions.
     */
    private String[] suggest(String word, int sugs)
        throws IOException {
        SpellChecker sc = new SpellChecker(index);
        sc.indexDictionary(new LuceneDictionary(reader, "content"),
                new IndexWriterConfig(analyzer), false);

        return sc.suggestSimilar(word, sugs, reader, "content", SuggestMode.SUGGEST_ALWAYS);
    }
}
