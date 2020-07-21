package com.stackoverflow;

import com.stackoverflow.helper.ProgressBar;
import com.stackoverflow.searching.InfoCustom;
import edu.gslis.lucene.main.config.QueryConfig;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import com.stackoverflow.searching.QueryLoader;
import com.stackoverflow.searching.RelevanceMarker;
import com.stackoverflow.searching.SearchEngine;
import com.stackoverflow.searching.MyCustomAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.retrievable.lucene.searching.expansion.Rocchio;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.lucene.search.similarities.BM25Similarity;

public class Main {
    public final static int SUGGESTIONS = 5;

    public static void main(String[] args) throws Exception {
        ArgumentParser parser = ArgumentParsers.newFor("Document Retrieval").build()
                .defaultHelp(true)
                .description("Document Retrieval");
        parser.addArgument("-t", "--type")
                .choices("standard", "custom").setDefault("standard")
                .help("Choose which analyzer you want to use: standard or custom");
        parser.addArgument("-l", "--loc")
                .setDefault("smallPosts")
                .help("Location of the xml files");
        parser.addArgument("-w", "--worddelimiter")
                .choices("true", "false").setDefault("true")
                .help("Custom analyzer using word delimiter");
        parser.addArgument("-tr", "--trim")
                .choices("true", "false").setDefault("true")
                .help("Custom analyzer using trim");
        parser.addArgument("-p", "--porter")
                .choices("true", "false").setDefault("true")
                .help("Custom analyzer using a porter stemmer");
        parser.addArgument("-n", "--ngram")
                .setDefault("0")
                .help("Custom analyzer using a ngrams where n is the length");
        parser.addArgument("-s", "--synonym")
                .choices("true", "false").setDefault("true")
                .help("Custom analyzer using a synonym filter");
        parser.addArgument("-r", "--rocchio")
                .choices("true", "false").setDefault("false")
                .help("Enable pseudo-relevance feedback using the Rocchio algorithm");
        parser.addArgument("--alpha")
                .setDefault("1.0")
                .help("Alpha value for the Rocchio algorithm");
        parser.addArgument("--beta")
                .setDefault("0.75")
                .help("Beta value for the Rocchio algorithm");
        parser.addArgument("--k1")
                .setDefault("1.2")
                .help("K1 value for the Rocchio algorithm");
        parser.addArgument("--b")
                .setDefault("0.75")
                .help("B value for the Rocchio algorithm");
        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        String type = ns.getString("type");
        String loc = ns.getString("loc");
        boolean w = Boolean.parseBoolean(ns.getString("worddelimiter"));
        boolean t = Boolean.parseBoolean(ns.getString("trim"));
        boolean p = Boolean.parseBoolean(ns.getString("porter"));
        int n = Integer.parseInt(ns.getString("ngram"));
        boolean s = Boolean.parseBoolean(ns.getString("synonym"));
        boolean rocchio = Boolean.parseBoolean(ns.getString("rocchio"));
        float alpha = Float.parseFloat(ns.getString("alpha"));
        float beta = Float.parseFloat(ns.getString("beta"));
        float k1 = Float.parseFloat(ns.getString("k1"));
        float b = Float.parseFloat(ns.getString("b"));
        InfoCustom info = new InfoCustom(w, t, p, n, s);
        Analyzer analyzer = null;
        if(type.equals("standard")) {
            analyzer = new StandardAnalyzer();
        } else if (type.equals("custom")) {
            analyzer = new MyCustomAnalyzer(info);
        } else {
            System.out.println("Invalid type parameter");
            System.exit(1);
        }

        SearchEngine engine;
        Rocchio expander = null;
        if(rocchio) {
            engine = new SearchEngine(loc, analyzer, new BM25Similarity(k1, b));
            expander = new Rocchio(alpha, beta, k1, b);
        } else {
            engine = new SearchEngine(loc, analyzer);
        }
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


        List<QueryConfig> queries = queryLoader.readQueries("data/suggestionsQuery.txt", SUGGESTIONS);



        System.out.println("Searching...");
        ProgressBar pb = new ProgressBar(0, queries.size());
        pb.print();
        // Write all matches to file in the same format as the manual labeling happened
        List<String> labels = new ArrayList<>();
        for (QueryConfig query : queries) {
            if(rocchio)  {
                expander.expandQuery(engine.getSearcher(), query, engine.getReader().numDocs(), 200);
            }
            Map<Double, Document> documents = engine.search(query);

            // Store for file writing
            List<String> ids = new ArrayList<>();
            for (Map.Entry<Double, Document> pair : documents.entrySet()) {
                ids.add(SearchEngine.questionID(pair.getValue().get("name")) + "," + pair.getKey());
            }

            String results = String.format("%02d", Integer.parseInt(query.getNumber())) + ":" +
                    String.join(";", ids);
            labels.add(results);

            pb.next();
            pb.print();
        }
        pb.end();
        BufferedWriter writer = new BufferedWriter(new FileWriter("data/labels.txt"));
        writer.write(String.join("\n", labels));
        writer.close();
    }
}
