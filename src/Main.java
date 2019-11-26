import jdk.jshell.spi.ExecutionControl;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
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
    private static void addDoc(IndexWriter w, String name, String title, String body, String tags, String answers)
            throws IOException {
        Document doc = new Document();
        doc.add(new TextField("name", name, Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("body", body, Field.Store.YES));
        doc.add(new TextField("tags", tags.replace(",", " "), Field.Store.YES));
        doc.add(new TextField("answers", answers, Field.Store.YES));
        w.addDocument(doc);
    }

    private static void createDocs(String path, Analyzer analyzer, Directory index)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);           // Overwrite existing files
        File dir = new File(path);                                       // The directory to search in

        IndexWriter w = new IndexWriter(index, config);
        File[] files = dir.listFiles();
        if(files != null) {
            for(File file: files) {
                if(file.isFile()) {
                    org.w3c.dom.Document doc = XML.parse(file);
                    String title = XML.xpath("/qroot/question/Title", doc).item(0).getTextContent();
                    String body = XML.xpath("/qroot/question/Body", doc).item(0).getTextContent();
                    String tags = XML.xpath("/qroot/question/Tags", doc).item(0).getTextContent();
                    String answers = "";
                    NodeList items = XML.xpath("/qroot/answer/Body", doc);
                    for(int i = 0; i < items.getLength(); ++i) {
                        answers += "\n\n" + items.item(i).getTextContent();
                    }
                    addDoc(w, file.getName(), title, body, tags, answers);
                }
            }
        }
        w.close();
    }

    private static String fileContents(File file) throws FileNotFoundException {
        Scanner input = new Scanner(file);
        StringBuilder builder = new StringBuilder();

        while(input.hasNext()){
            builder.append(input.nextLine());
        }

        return builder.toString();
    }

    private static Query getQuery(String old, Directory index, IndexReader reader, Analyzer analyzer)
            throws IOException, ParseException {
        String querystr = Helper.queryTransform(old, 1, index, reader, analyzer);
        return new MultiFieldQueryParser(new String[]{"body", "tags", "answers"}, analyzer).parse(querystr);
    }

    /**
     * Create a matrix concerning the performance on scoring the questions in a directory with its title as
     * (transformed) query. It does not return anything, but rather generates a JSON-file which contains the time it
     * took for each question to score and its corresponding scoring matrix, encoded behind two keys, as illustrated
     * below. It also includes a quick reference for each index in the matrix and the corresponding question it refers
     * to. This allows a more flexible and less overhead-heavy file on the whole.
     *
     * This file can be used as the input in some python script for showing the corresponding plots graphical
     * representations.
     *
     * Example results.json :
     *      {
     *          "scores": {
     * 		        "0": {
     * 			        "0": 12.510421,
     * 			        "20": 5.111517
     *              },
     * 		        "6": {
     * 			        "6": 8.548369
     *              },
     *              ...
     *          },
     *          "idxs": ["4", "9", "14", "42", "16", "59", ...],
     *          "time": [0.15608598, 0.12798472, 0.050792, 0.107817546, 0.074838094, ...]
     *      }
     *
     * The matrix can be reconstructed as follows:
     *      1)  Create a NxN matrix of 0's, where N = size of idxs
     *      2)  Go through the scores object and use its keys to identify the rows and say its value object
     *          is called V.
     *      3)  Use V's keys for the columns and its values for the scores.
     *      4)  Each row's and column's index of the matrix corresponds to the file with question id found in idxs,
     *          at the index of the row/column index.
     * For instance, in the example, row and column 0 correspond to question4.xml, which took 0.15 seconds to run
     * and yielded the score 12.510421 for its own title as (transformed) query.
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
        createDocs(directory, analyzer, index);

        // Search with the query
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);

        List<String> idxs = new ArrayList<>();
        // TreeMap instead of HashMap to make sure keys are sorted
        Map<Integer, Map<Integer, Float>> scores = new TreeMap<>();
        List<Float> time = new ArrayList<>();

        File dir = new File(directory);
        ArrayList<File> files = new ArrayList<>(Arrays.asList(Objects.requireNonNull(dir.listFiles())));
        int cnt = 0;
        for(File value : files) {
            if (value.isFile()) {
                ++cnt;
            }
        }
        System.out.println("Working...");
        for(int i = 0; i < files.size(); ++i) {
            File file = files.get(i);
            System.out.print("\r\tProgress: (" + (i+1) + " of " + files.size() + "); " +
                    ((float)(i+1) / (float)files.size() * 100) + "% Done.");
            if(file.isFile()) {
                // Parse File
                String name = file.getName();
                idxs.add(name.replace("question", "").replace(".xml", ""));
                NodeList nodes = XML.xpath("/qroot/question/Title", XML.parse(file));
                String title = nodes.item(0).getTextContent();

                // Get Query and Time it
                long startTime = System.nanoTime();
                Query q = getQuery(title, index, reader, analyzer);
                TopDocs docs = searcher.search(q, cnt);
                ScoreDoc[] hits = docs.scoreDocs;
                long endTime = System.nanoTime();
                long duration = (endTime - startTime);
                float secs = duration / (float)(1000*1000*1000);
                time.add(secs);

                // Fill the Matrix with zeros
                Map<Integer, Float> score = new HashMap<>();

                // Set the correct scores
                for(ScoreDoc hit: hits) {
                    int id = hit.doc;
                    Document d = searcher.doc(id);
                    // Get the consistent id from the files ArrayList
                    int fix = files.indexOf(new File(directory + "/" + d.get("name")));
//                    System.out.println("SCORE OF " + d.get("name") + " (" + fix + "): " + hit.score);
                    score.put(fix, hit.score);
                }
                if(!score.isEmpty()) {
                    scores.put(i, score);
                }
            }
        }
        System.out.print("\n");
        writeToJson(idxs, scores, time);
    }

    private static void writeToJson(List<String> idxs, Object scores, List<Float> time)
            throws IOException {
        // Make sure the json is structured correctly
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("idxs", idxs);
//        Helper.check(idxs.size(), scores.size());
//        for(List<Map<String, Float>> lf: scores) {
//            Helper.check(idxs.size(), lf.size());
//        }
        hm.put("scores", scores);
        Helper.check(idxs.size(), time.size());
        hm.put("time", time);

        try {
            String json = Helper.toJSON(hm);

            BufferedWriter writer = new BufferedWriter(new FileWriter("results.json"));
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

        performance("smallPosts", analyzer, index);
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
