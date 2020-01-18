import jdk.jshell.spi.ExecutionControl;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    private static ProgressBar spb;
    private static Map<String, Integer> searchCache = new HashMap<>();
//    private static BooleanSimilarity similarity = new BooleanSimilarity();
    private static TFIDFSimilarity similarity = new ClassicSimilarity();
    private static boolean otherSim = true;
    private static FieldType freqVecStorer;

    private static Set<String> queries = new TreeSet<>();

    static {
        freqVecStorer = new FieldType(TextField.TYPE_STORED);
        freqVecStorer.setStoreTermVectors(true);
    }

    private static void addDoc(IndexWriter w, String name, String title, String body, String tags, String answers)
            throws IOException {
        Document doc = new Document();
        doc.add(new Field("name", name, freqVecStorer));
        doc.add(new Field("title", title, freqVecStorer));
        doc.add(new Field("body", body, freqVecStorer));
        doc.add(new Field("tags", tags.replace("  ", " "), freqVecStorer));
        doc.add(new Field("answers", answers, freqVecStorer));
        w.addDocument(doc);
        Set<Set<String>> prms = Helper.tagsToQueries(tags);
        for(Set<String> ls: prms) {
            queries.add(String.join(" ", ls));
        }
        searchCache.put(name, searchCache.size());
    }

    private static void createDocs(List<File> files, Analyzer analyzer, Directory index)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);           // Overwrite existing files
        if (otherSim) {
            config.setSimilarity(similarity);
        }

        IndexWriter w = new IndexWriter(index, config);
        for(File file: files) {
            if(file.isFile()) {
                addParsedDoc(file, w);
                spb.next();
                spb.print();
            } else {
                w.close();
                throw new IOException("Impossible to have a directory at this point!");
            }
        }
        w.close();

        queries.removeIf(s -> !s.matches("[a-zA-Z ]+"));
    }

    private static void addParsedDoc(File file, IndexWriter w) throws
            ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        org.w3c.dom.Document doc = XML.parse(file);
        String title = XML.xpath("/qroot/question/Title", doc).item(0).getTextContent();
        String body = XML.xpath("/qroot/question/Body", doc).item(0).getTextContent();
        String tags = XML.xpath("/qroot/question/Tags", doc).item(0).getTextContent();
        String answers = "";
        NodeList items = XML.xpath("/qroot/answer/Body", doc);
        for(int i = 0; i < items.getLength(); ++i) {
            answers += "\n\n" + Helper.transform(items.item(i).getTextContent());
        }
        addDoc(w, file.getName(), Helper.transform(title), Helper.transform(body),
                Helper.transform(tags.replace(",", " ")), answers);
    }

    private static Query getQuery(String old, Directory index, IndexReader reader, Analyzer analyzer)
            throws IOException, ParseException {
        String querystr = Helper.queryTransform(old, 5, index, reader, analyzer);
        MultiFieldQueryParser mfqp = new MultiFieldQueryParser(new String[]{"body", "answers"}, analyzer);
        mfqp.setDefaultOperator(QueryParser.Operator.OR);
        return mfqp.parse(querystr);
    }

    private static List<File> index(String directory, Analyzer analyzer, Directory index, int cnt)
            throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        System.out.println("Loading files...");
        ArrayList<File> files = pickFiles(loadFiles(directory), cnt, 420);

        System.out.println("Creating Index...");
        spb = new ProgressBar(cnt);
        spb.start();
        spb.print();
        createDocs(files, analyzer, index);
        spb.end();
        System.out.println("Index Created");

        return files;
    }

    private static Map<Double, Document> advancedSearch(String query, List<String> relevant, Analyzer analyzer,
                                                        IndexSearcher searcher, int cnt,
                                                        Number alpha, Number beta, Number gamma)
            throws IOException {

        IndexReader reader = searcher.getIndexReader();
        Vector vNew = rocchio(relevant, reader, query, alpha, beta, gamma);
        try {
            Query q = vNew.toQuery(analyzer);
            return search(searcher, q, cnt);
        } catch(ParseException e) {
            System.out.println("\tEXCEPTION!");
            return new HashMap<Double, Document>();
        }
    }

    private static Map<Double, Document> search(IndexSearcher searcher, Query q, int cnt) throws IOException {
        TopDocs docs = searcher.search(q, cnt);
        ScoreDoc[] hits = docs.scoreDocs;
        Map<Double, Document> results = new TreeMap<>();
        for(ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document d = searcher.doc(docId);
            double score = hit.score;
            results.put(score, d);
        }
        return results;
    }

    private static Vector rocchio(List<String> relevant, IndexReader reader, String query,
                                  Number alpha, Number beta, Number gamma)
            throws IOException {
        List<Integer> rel = new ArrayList<>();
//        List<Integer> irrel = new ArrayList<>();
        for(String rels: relevant) {
            rel.add(searchCache.get(rels));
        }
//        for(Map.Entry<String, Integer> entry: searchCache.entrySet()) {
//            if(relevant.contains(entry.getKey())) {
//                rel.add(entry.getValue());
//            } else {
////                irrel.add(entry.getValue());
//            }
//        }
        PRF prf = new PRF(reader);
        Vector vRel = prf.sum(rel);
        Vector vIrr =new Vector();
        Vector vQry = prf.getQueryVector(query);

        return prf.rocchio(vQry, vRel, vIrr, alpha, beta, gamma);
    }

    /**
     * Creates a set of scores for the queries that are in the tagList.
     *
     * Basically, for each unique, alphabetic tag, a new query is created and scored
     * with our codes. The set of queries is also written to 'data/queries.txt'.
     *
     * Next, all documents are scored for this set of queries, which is stored in the
     * 'data/results.json' file. Each main key in this file represents the unique
     * question id (which is located in the filename) and each value is a dictionary
     * representing a succinct version of the score matrix. The pairs in these values
     * are the index of the query (in the order they are listed in the
     * 'data/queries.txt') and their corresponding score. When omitted, the scores are
     * zero.
     *
     * Finally, a 'data/relevant.json' file is also generated. For each query listed
     * in this file, you get a list of all the question ids that were marked relevant
     * for that query.
     *
     * @param analyzer      The Analyzer to use.
     * @param index         The Directory to use.
     */
    public static void performance(Analyzer analyzer, Directory index, List<File> files)
            throws IOException, ParseException {

        // Load all general information
        int cnt = files.size();
        List<String> qrs = new ArrayList<>(queries);
        writeTagList("data/queries.txt");
        Map<String, List<String>> relevant = new TreeMap<>();

        // Set IndexReader, IndexSearcher and Similarity measure
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        if (otherSim) {
            searcher.setSimilarity(similarity);
        }

        Map<String, Map<Integer, Double>> scores = new TreeMap<>();
        System.out.println("Searching for Query Matches");
        spb.reset(qrs.size());
        spb.start();
        spb.print();
        for(int qi = 0; qi < qrs.size(); ++qi) {
            String query = qrs.get(qi);

            for(Map.Entry<String, Integer> entry: searchCache.entrySet()) {
                String qid = entry.getKey().replace("question", "").replace(".xml", "");
                Document d = searcher.doc(entry.getValue());
                Set<Set<String>> prms = Helper.tagsToQueries(d.get("tags"));
                Set<String> ns = new TreeSet<>();
                for(Set<String> ls: prms) {
                    ns.add(String.join(" ", ls));
                }
                if(ns.contains(query)) {
                    if(relevant.containsKey(query)) {
                        relevant.get(query).add(qid);
                    } else {
                        List<String> val = new ArrayList<>();
                        val.add(qid);
                        relevant.put(query, val);
                    }
                }
            }

            List<String> rel = relevant.get(query);
            for(int i = 0; i < rel.size(); ++i) {
                rel.set(i, "question" + rel.get(i) + ".xml");
            }

            Map<Double, Document> res = advancedSearch(query, rel, analyzer, searcher, cnt,0.5, 0, 0.5);
//            Map<Double, Document> res = search(searcher, getQuery(query, index, reader, analyzer), cnt);
            for(Map.Entry<Double, Document> entry: res.entrySet()) {
                Document d = entry.getValue();
                double score = entry.getKey();
                String qname = d.get("name");
                String qid = qname.replace("question", "").replace(".xml", "");
                if(scores.containsKey(qid)) {
                    scores.get(qid).put(qi, score);
                } else {
                    Map<Integer, Double> sc = new HashMap<>();
                    sc.put(qi, score);
                    scores.put(qid, sc);
                }
            }

            spb.next();
            spb.print();
            System.out.print("\t SCORESIZE: " + scores.size());
        }
        writeToJson("data/results.json", scores);
        writeToJson("data/relevant.json", relevant);
        spb.end();
    }

    private static ArrayList<String> loadFiles(String directory) {
        ArrayList<String> res = new ArrayList<>();
        File dir = new File(directory);

        // Iterate via Strings for efficiency
        String[] fls = dir.list();
        for(String file: Objects.requireNonNull(fls)) {
            if(file.equals(".DS_Store")) { continue; } // MAC
            if(file.endsWith(".xml")) { // We're only interested in XML
                res.add(directory + "/" + file);
            } else {
                File f = new File(directory + "/" + file);
                if(f.isDirectory()) {
                    res.addAll(loadFiles(directory + "/" + file));
                }
            }
        }

        return res;
    }

    private static ArrayList<File> pickFiles(ArrayList<String> list, int count, int seed) {
        ArrayList<File> rres = new ArrayList<>();
        System.out.println("Total Dataset Size: " + list.size());

        // Pick <count> Documents (efficiency)
        if(list.size() < count) {
            for(String s : list) {
                rres.add(new File(s));
            }
        } else { // Pick Random
            Random rand = new Random(seed);
            HashSet<Integer> found = new HashSet<>();
            for(int i = 0; i < count; ++i) {
                int n;
                do {
                    n = rand.nextInt(list.size());
                } while(found.contains(n));
                found.add(n);
                rres.add(new File(list.get(n)));
            }
        }

        Collections.sort(rres); // Set the order
        return rres;
    }

    private static int indexOfFileByEnd(ArrayList<File> lst, String end) {
        for(int i = 0; i < lst.size(); ++i) {
            File file = lst.get(i);
            if(file.getName().endsWith(end)) {
                return i;
            }
        }
        return -1;
    }

    private static void writeToJson(String filename, Object object)
            throws IOException {
        try {
            String json = Helper.toJSON(object);

            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write(json);
            writer.close();
        } catch (ExecutionControl.NotImplementedException e) {
            e.printStackTrace();
        }
    }

    private static void writeTagList(String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        for(String tag: queries) {
            writer.write(tag + "\n");
        }
        writer.close();
    }

    private static List<String> getQueries(String filename) throws FileNotFoundException {
        List<String> queries = new ArrayList<>();
        Scanner sc = new Scanner(new File(filename));
        while(sc.hasNextLine()) {
            queries.add(sc.nextLine());
        }
        return queries;
    }

    public static void main(String[] args)
            throws IOException, ParseException, XPathExpressionException, SAXException, ParserConfigurationException,
            ExecutionControl.NotImplementedException, org.json.simple.parser.ParseException {
        // Create the documents to index
        StandardAnalyzer analyzer = new StandardAnalyzer();
//        MyCustomAnalyzer analyzer = new MyCustomAnalyzer();
        Directory index = new MMapDirectory(Paths.get(".search")); // Instead of deprecated RAMDirectory

        String loc = args.length == 0 ? "smallPosts" : args[0];

        int cnt = 10000;
        List<File> files = index(loc, analyzer, index, cnt);

        performance(analyzer, index, files);
//        writeToJson("data/cache.json", searchCache);

//        createDocs("/home/red/Software/lucene-8.3.0", analyzer, index);
//
//        // Search with the query
//        IndexReader reader = DirectoryReader.open(index);
//        IndexSearcher searcher = new IndexSearcher(reader);
//
//        // Build query out of stdin
//        String querystr = args.length > 0 ? args[0] : "there";
//        Query q = getQuery(querystr, index, reader, analyzer);
//
//        // Get all search results
//        int hitsPerPage = 10; // Limits the result count
//        TopDocs docs = searcher.search(q, hitsPerPage);
//        ScoreDoc[] hits = docs.scoreDocs;
//
//        // Spell checking
//        String[] s = Helper.suggest("There", 5, index, reader, analyzer);
//        System.out.println("\nSPELL CHECKER:");
//        for(String st: s) {
//            System.out.println("\t" + st);
//        }
//
//        // Show results
//        System.out.println("\nFound " + hits.length + " hits.");
//        for(int i = 0; i < hits.length; ++i) {
//            int docId = hits[i].doc;
//            float score = hits[i].score;
//            Document d = searcher.doc(docId);
//            System.out.println((i + 1) + ". (" + score + ")   " + d.get("title"));
//        }
    }
}
