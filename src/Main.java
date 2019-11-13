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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    private static void addDoc(IndexWriter w, String title, String content) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.YES));
        w.addDocument(doc);
    }

    private static void createDocs(Analyzer analyzer, Directory index) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);           // Overwrite existing files
        File dir = new File("/home/red/Software/lucene-8.3.0"); // The directory to search in

        IndexWriter w = new IndexWriter(index, config);
        File[] files = dir.listFiles();
        if(files != null) {
            for(File file: files) {
                if(file.isFile()) {
                    addDoc(w, file.getName(), fileContents(file));
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

    public static void main(String[] args) throws IOException, ParseException {
        // Create the documents to index
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory index = new MMapDirectory(Paths.get(".search")); // Replaces deprecated RAMDirectory

        createDocs(analyzer, index);

        // Build query out of stdin
        String querystr = args.length > 0 ? args[0] : "lucene";
        Query q = new MultiFieldQueryParser(new String[]{"title", "content"}, analyzer).parse(querystr);

        // Search with the query
        int hitsPerPage = 10; // Limits the result count
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        // Show results
        System.out.println("\nFound " + hits.length + " hits.");
        for(int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            float score = hits[i].score;
            Document d = searcher.doc(docId);
            System.out.println((i + 1) + ". (" + score + ")   " + d.get("title"));
        }
    }
}
