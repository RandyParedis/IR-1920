package com.stackoverflow;

import com.stackoverflow.helper.ProgressBar;
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

        // Adapt the arguments of the function to your likings :D
//        List<String> qsts = Arrays.asList("Django", "Django Development", "My very fancy query");
//        for(String qst: qsts) {
//            Query q = queryLoader.getQuery(qst, SUGGESTIONS);
//            Map<Double, Document> documents = engine.search(q);
//            System.out.println("\n" + documents.size() + " result(s) for '" + qst + "':");
//            for(Map.Entry<Double, Document> en: documents.entrySet()) {
//                System.out.println("\t" + en.getKey() + ": " + en.getValue().get("name"));
//            }
//            System.out.println();
//        }


        List<Query> queries = queryLoader.getQueries("data/suggestionsQuery.txt", SUGGESTIONS,
                (String q) -> q.substring(3));

        System.out.println("Testing Engine...");
        ProgressBar pb = new ProgressBar(0, queries.size());
        pb.print();
        // Write all matches to file in the same format as the manual labeling happened
        // TODO: do things with matched documents
        List<String> labels = new ArrayList<>();
        for(int qid = 0; qid < queries.size(); ++qid) {
            // Find the matching documents
            Query query = queries.get(qid);
            Map<Double, Document> documents = engine.search(query);
//            System.out.println("Query " + query.toString());


            // Store for file writing
            List<String> ids = new ArrayList<>();
            for(Map.Entry<Double, Document> pair: documents.entrySet()) {
                ids.add(SearchEngine.questionID(pair.getValue().get("name")) + "," + pair.getKey());
            }
            String results =  String.format("%02d", qid);

            // Special ids:
            if(qid == queries.size() - 2) { // 98 is Python
                results = "98";
            } else if(qid == queries.size() - 1) { // 99 is CPP
                results = "99";
            }
            results += ":" + String.join(";", ids);
            labels.add(results);
            pb.next();
            pb.print();
        }
        pb.end();
        BufferedWriter writer = new BufferedWriter(new FileWriter("data/labels.txt"));
        writer.write(String.join("\n", labels));
        writer.close();
//
//        // TODO: Mark as relevant and generate plot data
//        RelevanceMarker relevanceMarker = new RelevanceMarker();
    }
}