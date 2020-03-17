import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import searching.QueryLoader;
import searching.RelevanceMarker;
import searching.SearchEngine;

import java.util.Map;

public class Main {
    public final static int SUGGESTIONS = 5;

    public static void main(String[] args) throws Exception {
        String loc = args.length == 0 ? "smallPosts" : args[0];
        StandardAnalyzer analyzer = new StandardAnalyzer();

        SearchEngine engine = new SearchEngine(loc, analyzer);
        // TODO: change this so it uses the actual files instead of a subset.
//        engine.index();

        QueryLoader queryLoader = new QueryLoader(engine.getDirectory(), engine.getReader(), analyzer);
        Query query = queryLoader.getQuery("test this string", SUGGESTIONS);

        Map<Double, Document> documents = engine.search(query);

        // TODO: Mark as relevant and generate plot data
        RelevanceMarker relevanceMarker = new RelevanceMarker();
    }
}
