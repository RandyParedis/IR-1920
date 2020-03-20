package com.stackoverflow;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import com.stackoverflow.searching.QueryLoader;
import com.stackoverflow.searching.RelevanceMarker;
import com.stackoverflow.searching.SearchEngine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
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

        // Write all matches to file in the same format as the manual labeling happened
        // TODO: do things with matched documents
        List<String> labels = new ArrayList<>();
        for(int qid = 0; qid < queries.size(); ++qid) {
            // Find the matching documents
            Query query = queries.get(qid);
            Map<Double, Document> documents = engine.search(query);


            // Store for file writing
            List<String> ids = new ArrayList<>();
            for(Map.Entry<Double, Document> pair: documents.entrySet()) {
                ids.add(SearchEngine.questionID(pair.getValue().get("name")));
            }
            String results =  String.format("%02d", qid);

            // Special ids:
            if(qid == queries.size() - 2) { // 98 is Python
                results = "98";
            } else if(qid == queries.size() - 1) { // 99 is CPP
                results = "99";
            }
            results += ":" + String.join(",", ids);
            labels.add(results);
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter("data/labels.txt"));
        writer.write(String.join("\n", labels));
        writer.close();

        // TODO: Mark as relevant and generate plot data
        RelevanceMarker relevanceMarker = new RelevanceMarker();
    }
}
