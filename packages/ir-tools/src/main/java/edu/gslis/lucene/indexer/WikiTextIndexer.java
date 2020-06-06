package edu.gslis.lucene.indexer;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;

import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.lucene.index.IndexWriter;
import org.xml.sax.InputSource;

import edu.gslis.lucene.main.config.FieldConfig;



/**
 * Build a Lucene index given a mediawiki dump
 */
public class WikiTextIndexer extends Indexer {
    
    
    static final String PAGE_TAG = "page";
    
    public void buildIndex(IndexWriter writer, Set<FieldConfig> fields, String name,
            InputStream input) throws Exception 
    {  
        WikiIndexWriter dumpWriter = new WikiIndexWriter(writer, fields, this);
        System.out.println("Indexing " + name);
        XmlDumpReader wikiReader = null;
                    
        Reader reader= null;
        try {
            // Use commons-compress to auto-detect compressed formats
            InputStream ois = new BufferedInputStream(input);
            InputStream is = new CompressorStreamFactory().createCompressorInputStream(ois);                
            System.out.println("Auto-detected format");
            reader = new InputStreamReader(is, "UTF-8");

        } catch (Exception e) {
            try { 
                InputStream ois = new BufferedInputStream(input);
                // Try XZ directly, for grins
                InputStream is = new XZCompressorInputStream(ois);
                System.out.println("Reading XZ compressed text");
                reader = new InputStreamReader(is, "UTF-8");
            } catch (Exception e2) {
                System.out.println("Assuming UTF-8 encoded text");
                // Treat as uncompressed raw XML
                reader = new InputStreamReader(input);                    
            }
        }            
        wikiReader = new XmlDumpReader(new InputSource(reader), dumpWriter);
        wikiReader.readDump();
        reader.close();
    }    
}
        