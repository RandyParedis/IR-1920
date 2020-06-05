package edu.gslis.lucene.indexer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import streamcorpus_v3.Sentence;
import streamcorpus_v3.StreamItem;
import streamcorpus_v3.Tagging;
import streamcorpus_v3.Token;
import edu.gslis.lucene.main.config.FieldConfig;


/**
 * Builds a Lucene index from a thrift-formatted Stream corpus.
 *
 */
public class StreamCorpusIndexer extends Indexer 
{
    public final static String SERIF = "serif";
    /**
     * Recurses files in a directory
     */
    @Override
    public long buildIndex(IndexWriter writer, Set<FieldConfig> fields,
            File file) throws Exception 
    {
        long count = 0;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f: files) {
                count += buildIndex(writer, fields, f);
                
                if ((count % 1000) == 0)
                    System.out.println(count + " documents indexed");
            }
        }
        else {
            try
            {
                String name = file.getName();
                InputStream is = new FileInputStream(file);
                buildIndex(writer, fields, name, is);
                count++;
            } catch (Exception e) {
                System.out.println("Error processing file " + file.getAbsolutePath());
                e.printStackTrace();
            }
        }
        return count;

    }
     
    /**
     * Construct a Lucene index given an input stream from an optionally 
     * compressed thrift file.
     */
    public void buildIndex(IndexWriter writer, Set<FieldConfig> fields, String name,
            InputStream is) throws Exception 
    {
        Analyzer analyzer = writer.getAnalyzer();
                
        InputStream input;
        try {
            // Use commons-compress to detect compressed format
            InputStream ois = new BufferedInputStream(is);
            input = new CompressorStreamFactory().createCompressorInputStream(ois);                
        } catch (Exception e) {
            try { 
                // If necessary, try XZ directly
                InputStream ois = new BufferedInputStream(is);
                input = new XZCompressorInputStream(ois);
            } catch (Exception e2) {
                // Otherwise treat as uncompressed
                input = is;
            }
        }            
        
        // Setup the thrift inputstream
        TTransport inTransport = 
            new TIOStreamTransport(new BufferedInputStream(input));
        TBinaryProtocol inProtocol = new TBinaryProtocol(inTransport);
        inTransport.open();
        try 
        {
            Charset charset = Charset.forName("UTF-8");
            CharsetDecoder decoder = charset.newDecoder();

            // Run through items in the thrift file
            while (true) 
            {
                // One Lucene document per thrift item
                Document luceneDoc = new Document();

                final StreamItem item = new StreamItem();
                item.read(inProtocol);
                // We're only using the cleaned/visible text
                if (item.body == null || item.body.clean_visible == null) {
                    // Can't work with empty text...
                    continue;
                }
                    
                String streamId = item.stream_id;
                String timestamp = String.valueOf((long)item.getStream_time().getEpoch_ticks());
                String clean_visible = item.getBody().getClean_visible();
                // Bug in KBA processing leaves some tags
                clean_visible = clean_visible.replaceAll("<[^>]*>", ""); 
                String stream_source = item.getSource();
                        
                String url = "";
                if (item.abs_url != null) {
                    url = decoder.decode(item.abs_url).toString();
                }
                        
                String docnoField = "";
                
                for (FieldConfig field: fields) {
                    String source = field.getSource();

                    if (!StringUtils.isEmpty(source))
                    {
                        if (source.equals("doc_id"))  {
                            docnoField = field.getName();
                            addField(luceneDoc, field, streamId, analyzer);
                        }
                        else if (source.equals("timestamp")) 
                            addField(luceneDoc, field, timestamp, analyzer);
                        else if (source.equals("body")) 
                            addField(luceneDoc, field, clean_visible, analyzer);
                        else if (source.equals("source")) 
                            addField(luceneDoc, field, stream_source, analyzer);
                        else if (source.equals("url")) 
                            addField(luceneDoc, field, url, analyzer);
                    }
                }
                writer.updateDocument(new Term(docnoField, streamId), luceneDoc);
            }
    
        } catch (TTransportException te) {
            if (te.getType() == TTransportException.END_OF_FILE) {
                //System.out.println("*** EOF ***");
            } else {
                throw te;
            }
        }
        inTransport.close(); 
        System.out.println("Finished " + name);
    }
    
    /**
     * Simple method to dump the Serif POS, dependency path and co-ref data
     * to standard out.
     * @param item
     */
    public void dumpSerif(StreamItem item) 
    {
        List<Sentence> sentences = item.body.getSentences().get(SERIF);
        int sentenceId = 0;
        for (Sentence sentence: sentences) {
            System.out.println("Sentence " + sentenceId);
            Iterator<Token> it = sentence.getTokensIterator();
            while (it.hasNext()) {
                Token token = it.next();
                int mentionId = token.mention_id;
                int parent = token.getParent_id();
                String pos = token.getPos();
                String depPath = token.getDependency_path();
                String term = token.getToken();
                int id = token.getToken_num();
                if (mentionId != -1) 
                    System.out.println("\t" + term + "(" + sentenceId + "," + id + "," + mentionId + "," +  pos + "," + depPath + "," + parent + ")");
            }
            sentenceId ++;
        }
        
       // Map<String, List<Relation>> relationMap = item.body.getRelations();
        //List<Relation> relations = relationMap.get(SERIF);
        
        Map<String, Tagging> taggingMap = item.body.getTaggings();
        for (String key: taggingMap.keySet()) {
            Tagging tagging = taggingMap.get(key);
            System.out.println(tagging.toString());
        }   
    }
}
