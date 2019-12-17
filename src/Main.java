import jdk.jshell.spi.ExecutionControl;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
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

    private static void addDoc(IndexWriter w, String name, String title, String body, String tags, String answers)
            throws IOException {
        Document doc = new Document();
        doc.add(new TextField("name", name, Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("body", body, Field.Store.YES));
        doc.add(new TextField("tags", tags.replace(",", " "), Field.Store.YES));
        doc.add(new TextField("answers", answers, Field.Store.YES));
        w.addDocument(doc);
        searchCache.put(name, searchCache.size());
    }

    private static void createDocs(List<File> files, Analyzer analyzer, Directory index)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);           // Overwrite existing files

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

    /**
     * Create a matrix concerning the performance on scoring the questions in a directory with its title as
     * (transformed) query. It does not return anything, but rather generates JSON-files which contain the score
     * these files gave themselves and how much they gave their opposite.
     *
     * Example results.json :
     *      {
     *          "scores": {
     * 		        "0": [12.510421, 5.111517],
     * 		        "6": [8.548369, 0.0],
     *              ...
     *          },
     *          "idxs": ["4", "9", "14", "42", "16", "59", ...]
     *      }
     *
     * For instance, in the example, question with index 0 (accoring to the idxs list, this is question 4) gave itself
     * a score of 12.510421, but also scored high on the negative space (5.111517).
     *
     * @param directory     The directory to look inside of.
     * @param analyzer      The Analyzer to use.
     * @param index         The Directory to use.
     *
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws ParseException
     */
    public static void performance(String directory, Analyzer analyzer, Directory index)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, ParseException {

        System.out.println("Loading files...");
        int cnt = 10000;
        ArrayList<File> files = pickFiles(loadFiles(directory), cnt - 1, 420);
        File neg_space = new File("data/question_NS.xml");
        files.add(neg_space);
        searchCache.put(neg_space.getName(), searchCache.size());

        System.out.println("Creating Index...");
        spb = new ProgressBar(cnt);
        spb.start();
        spb.print();
        createDocs(files, analyzer, index);
        spb.end();
        System.out.println("Index Created");

        // Search with the query
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);

        List<String> idxs = new ArrayList<>();
//        for(File f: files) {
//            idxs.add(f.getPath().replace(directory, "")
//                    .replace("question", "")
//                    .replace(".xml", ""));
//        }
        Map<Integer, List<Float>> scores = new TreeMap<>();

//        System.out.println("Searching for Query Matches");
//        Query q = getQuery("python", index, reader, analyzer);
//        TopDocs docs = searcher.search(q, cnt);
//        ScoreDoc[] hits = docs.scoreDocs;

        System.out.println("Generating Output Files");
//        spb.reset(hits.length);
        spb.reset(cnt);
        spb.start();
        for(int i = 0; i < cnt; ++i) {
            spb.set(i+1);
            spb.print();
            File file = files.get(i);
            if(file.isFile()) {
                // Parse File
                String name = file.getName();
                idxs.add(name.replace("question", "").replace(".xml", ""));
                NodeList nodes = XML.xpath("/qroot/question/Title", XML.parse(file));
                String title = nodes.item(0).getTextContent();

                // Get Query and Score it
                Query q = getQuery(title, index, reader, analyzer);
                TopDocs docs = searcher.search(q, cnt);
                ScoreDoc[] hits = docs.scoreDocs;
                float score = 0.0f;
                float score_NS = 0.0f;
                for(ScoreDoc hit : hits) {
                    int docId = hit.doc;
                    Document d = searcher.doc(docId);
                    String dname = d.get("name");
                    if (dname.equals(name)) {
                        score = hit.score;
                    } else if (dname.equals(neg_space.getName())) {
                        score_NS = hit.score;
                    }
                }
//                Explanation explain = searcher.explain(q, searchCache.get(name));
//                float score = (float) explain.getValue();
//                explain = searcher.explain(q, searchCache.get(neg_space.getName()));
//                float score_NS = (float) explain.getValue();

                if(score > 0.0f || score_NS > 0.0f) {
                    scores.put(i, Arrays.asList(score, score_NS));
                }
            }
//            int docId = hits[i].doc;
//            float score = hits[i].score;
//            Document d = searcher.doc(docId);
//            String name = d.get("name");
//            scores.put(searchCache.get(name), score);
        }
        writeToJson("data/results.json", directory, idxs, scores);
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
                res.addAll(loadFiles(directory + "/" + file));
            }
        }

        return res;
    }

    private static ArrayList<File> pickFiles(ArrayList<String> list, int count, int seed) {
        ArrayList<File> rres = new ArrayList<>();

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

    private static void writeToJson(String filename, String directory, List<String> idxs, Object scores)
            throws IOException {
        // Make sure the json is structured correctly
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("directory", directory);
        hm.put("idxs", idxs);
        hm.put("scores", scores);

        try {
            String json = Helper.toJSON(hm);

            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write(json);
            writer.close();
        } catch (ExecutionControl.NotImplementedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
            throws IOException, ParseException, XPathExpressionException,
            SAXException, ParserConfigurationException {
        // Create the documents to index
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory index = new MMapDirectory(Paths.get(".search")); // Replaces deprecated RAMDirectory

        String loc = args.length == 0 ? "smallPosts" : args[0];

        performance(loc, analyzer, index);
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
