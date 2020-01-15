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
        doc.add(new Field("tags", tags.replace(",", " "), freqVecStorer));
        doc.add(new Field("answers", answers, freqVecStorer));
        w.addDocument(doc);
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
        MultiFieldQueryParser mfqp = new MultiFieldQueryParser(new String[]{"body", "tags", "answers"}, analyzer);
        mfqp.setDefaultOperator(QueryParser.Operator.OR);
        return mfqp.parse(querystr);
    }

    private static List<File> setUp(String directory, Analyzer analyzer, Directory index, int cnt)
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

    /**
     * Creates a set of scores for the queries located in 'data/queries.txt'.
     *
     * The result is a json file where each key represents the question index and each
     * value its set of corresponding scores. The first element of the set represents
     * the score for the first query, its second element the second query etc...
     *
     * @param analyzer      The Analyzer to use.
     * @param index         The Directory to use.
     */
    public static void performance(Analyzer analyzer, Directory index, List<File> files)
            throws IOException, ParseException {

        // Load all general information
        int cnt = files.size();
        List<String> queries = getQueries("data/queries.txt");

        // Set IndexReader, IndexSearcher and Similarity measure
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        if (otherSim) {
            searcher.setSimilarity(similarity);
        }

        Map<Integer, List<Double>> scores = new TreeMap<>();
        System.out.println("Searching for Query Matches");
        spb.reset(queries.size());
        spb.start();
        spb.print();
        for(int qi = 0; qi < queries.size(); ++qi) {
            String query = queries.get(qi);
            Query q = getQuery(query, index, reader, analyzer);
            TopDocs docs = searcher.search(q, cnt);
            ScoreDoc[] hits = docs.scoreDocs;
            for(ScoreDoc hit : hits) {
                int docId = hit.doc;
                Document d = searcher.doc(docId);
                String qname = d.get("name");
                int qid = Integer.parseInt(qname.replace("question", "")
                        .replace(".xml", ""));
                double score = hit.score;
                if(scores.containsKey(qid)) {
                    scores.get(qid).set(qi, score);
                } else {
                    List sc = new ArrayList<Double>();
                    for(int i = 0; i < queries.size(); ++i) {
                        sc.add(0);
                    }
                    sc.set(qi, score);
                    scores.put(qid, sc);
                }
            }
            spb.next();
            spb.print();
        }
        writeToJson("data/results.json", scores);
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
        List<File> files = setUp(loc, analyzer, index, cnt);
        performance(analyzer, index, files);
        writeToJson("data/cache.json", searchCache);

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
