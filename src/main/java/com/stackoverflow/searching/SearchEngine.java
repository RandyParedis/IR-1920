package com.stackoverflow.searching;

import com.stackoverflow.helper.Helper;
import com.stackoverflow.helper.ProgressBar;
import com.stackoverflow.helper.XML;
import edu.gslis.lucene.main.config.QueryConfig;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * The {@code SearchEngine} class is a class that will do the actual com.stackoverflow.searching.
 * It has an index phase, in which it will index all documents and a search
 * phase that will be able to collect the top-k documents matching a Query,
 * based on some predefined settings.
 */
public class SearchEngine {
    private Analyzer analyzer;
    private String searchFolder;
    private Directory directory = new MMapDirectory(Paths.get(".search"));
    private Similarity similarity;
    private IndexReader reader;
    private IndexSearcher searcher;
    private boolean wasIndexed = false;

    final private int COUNT = 500;

    private ProgressBar spb = new ProgressBar();
    private FieldType freqVecStorer;

    /**
     * {@code SearchEngine} constructor.
     * @param location      The location where to search.
     * @param analyzer      The analyzer to use.
     * @param similarity    The similarity of the engine.
     */
    public SearchEngine(String location, Analyzer analyzer, Similarity similarity) throws IOException {
        this.searchFolder = location;
        this.analyzer = analyzer;
        this.similarity = similarity;

        freqVecStorer = new FieldType(TextField.TYPE_STORED);
        freqVecStorer.setStoreTermVectors(true);

        // Prevents error
        IndexWriter iwriter = new IndexWriter(directory, new IndexWriterConfig(analyzer));
        iwriter.commit();
        iwriter.close();

        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
    }

    /**
     * {@code SearchEngine} constructor.
     *
     * It will automatically set the {@code Similarity} to the {@code ClassicSimilarity}.
     *
     * @param location  The location where to search.
     * @param analyzer  The analyzer to use.
     */
    public SearchEngine(String location, Analyzer analyzer) throws IOException {
        this(location, analyzer, new ClassicSimilarity());
    }



    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public Directory getDirectory() {
        return directory;
    }

    public IndexReader getReader() {
        return reader;
    }

    public IndexSearcher getSearcher() {
        return searcher;
    }



    /**
     * Does the indexing phase.
     *
     * - First, it will pick at random {@code COUNT} documents to index.
     *   This is done with a seed of 420, to make the system deterministic.
     * - Next, it will start creating the index for these documents, via calling
     *   the {@code createDocs} method.
     *
     * @see SearchEngine#createDocs
     */
    public void index() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        System.out.println("Loading files...");
        ArrayList<File> files = Helper.pickFiles(Helper.loadFiles(searchFolder), COUNT, 420);

        System.out.println("Creating Index...");
        spb.reset(COUNT);
        spb.start();
        spb.print();
        createDocs(files);
        spb.end();
        System.out.println("Index Created");
        wasIndexed = true;
    }

    /**
     * Retrieves the question id from the filename.
     * @param filename  The filename to retrieve the question id from.
     * @return The question id as a {@code String}.
     */
    public static String questionID(String filename) {
        String pattern = "^question(\\d+)\\.xml$";
        return filename.replaceAll(pattern, "$1");
    }

    /**
     * Fills the index with all the documents listed in files by first parsing them.
     * @param files     The files to index.
     */
    private void createDocs(List<File> files)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);           // Overwrite existing files
        config.setSimilarity(similarity);

        IndexWriter w = new IndexWriter(directory, config);
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

    /**
     * Parse a file using {@code xpath} and add it to the index.
     * @param file  The file to add to the index.
     * @param w     The {@code IndexWriter} to use for adding it.
     * @see SearchEngine#addDoc
     */
    private void addParsedDoc(File file, IndexWriter w) throws
            ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        org.w3c.dom.Document doc = XML.parse(file);
        String title = XML.xpath("/qroot/question/Title", doc).item(0).getTextContent();
        String body = XML.xpath("/qroot/question/Body", doc).item(0).getTextContent();
        String tags = XML.xpath("/qroot/question/Tags", doc).item(0).getTextContent();
        StringBuilder answers = new StringBuilder();
        NodeList items = XML.xpath("/qroot/answer/Body", doc);
        for(int i = 0; i < items.getLength(); ++i) {
            answers.append("\n\n").append(items.item(i).getTextContent());
        }
        addDoc(w, file.getName(), title, body, tags.replace(",", " "), answers.toString());
    }

    /**
     * Adds a document to the index. Is used in the indexing phase.
     * @param w         The {@code IndexWriter} to use.
     * @param name      The name of the file that was indexed.
     *                  This will be in the form of {@code question{id}.xml}
     * @param title     The title of the question, found in the {@code Title} tag.
     * @param body      The body of the question, found in the {@code Body} tag.
     * @param tags      The tags of the question, space-separated.
     * @param answers   The answers to the question, separated from one another with
     *                  whitespace.
     * @see SearchEngine#index
     */
    private void addDoc(IndexWriter w, String name, String title, String body, String tags, String answers)
            throws IOException {
        Document doc = new Document();
        tags = tags.replace("  ", " ");
        String text = title + "\n" + body + "\n" + tags + answers;
        doc.add(new Field("name", name, freqVecStorer));
        doc.add(new Field("title", title, freqVecStorer));
        doc.add(new Field("body", body, freqVecStorer));
        doc.add(new Field("tags", tags, freqVecStorer));
        doc.add(new Field("answers", answers, freqVecStorer));
        doc.add(new Field("text", text, freqVecStorer));  // For Rocchio
        w.addDocument(doc);
    }



    /**
     * Searches the index for the top-k documents.
     * @param q     The query to search for.
     * @param k     The amount of documents that need to be returned at most.
     * @return  A {@code Map<Double, Document>} that maps the score to a Document.
     *          This is a {@code TreeMap}, meaning that an iterator over this {@code Map}
     *          will automatically be sorted on score.
     * @throws RuntimeException when the engine wasn't indexed yet.
     */
    public Map<Double, Document> search(Query q, int k) throws RuntimeException, IOException {
        if(!wasIndexed) {
            throw new RuntimeException("You cannot search when the engine hasn't been indexed yet.");
        }
        TopDocs docs = searcher.search(q, k);
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

    /**
     * Searches the index for all the documents.
     * Is an overloaded call to {@code search(q, COUNT)}
     * @param q     The query to search for.
     * @return  A {@code Map<Double, Document>} that maps the score to a Document.
     *          This is a {@code TreeMap}, meaning that an iterator over this {@code Map}
     *          will automatically be sorted on score.
     */
    public Map<Double, Document> search(Query q) throws IOException {
        return search(q, COUNT);
    }

    public Map<Double, Document> search(QueryConfig queryConfig)
            throws IOException, ParseException {
        Query query;
//        QueryParser queryParser = new QueryParser("text", new StandardAnalyzer());
//        query = queryParser.parse(queryConfig.getText());
        MultiFieldQueryParser mfqp = new MultiFieldQueryParser(QueryLoader.FIELDS, analyzer);
        mfqp.setDefaultOperator(QueryParser.Operator.OR);
        query = mfqp.parse(queryConfig.getText());
        return search(query);
    }
}
