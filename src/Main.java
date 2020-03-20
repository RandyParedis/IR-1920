import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import searching.QueryLoader;
import searching.RelevanceMarker;
import searching.SearchEngine;

import java.util.List;
import java.util.Map;

public class Main {
    public final static int SUGGESTIONS = 5;

    public static void main(String[] args) throws Exception {
        String loc = args.length == 0 ? "smallPosts" : args[0];
        StandardAnalyzer analyzer = new StandardAnalyzer();

        SearchEngine engine = new SearchEngine(loc, analyzer);
        engine.index();

        // Load all queries
        QueryLoader queryLoader = new QueryLoader(engine.getDirectory(), engine.getReader(), analyzer);
        List<Query> queries = queryLoader.getQueries("suggestionsQuery.txt", SUGGESTIONS,
                (String q) -> q.substring(3));

        // TODO: Store results in a file, so it can be parsed by Python
        //       In Python, it will be compared against the manually labeled files.
        for(Query query: queries) {
            Map<Double, Document> documents = engine.search(query);

            // TODO: Mark as relevant and generate plot data
            RelevanceMarker relevanceMarker = new RelevanceMarker();
        }
    }
}
