package edu.gslis.lucene.main;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import edu.gslis.lucene.indexer.Indexer;
import edu.gslis.lucene.indexer.StreamCorpusIndexer;
import edu.gslis.lucene.indexer.TikaIndexer;
import edu.gslis.lucene.indexer.TrecTextIndexer;
import edu.gslis.lucene.indexer.WikiTextIndexer;
import edu.gslis.lucene.main.config.CorpusConfig;
import edu.gslis.lucene.main.config.FieldConfig;
import edu.gslis.lucene.main.config.IndexConfig;


/**
 * Lucene-backed index builder.  Requires a Yaml-based configuration file.
 * 
 * A few things to note:
 * 
 * 1. Analyzer.  This assumes any configurable analyzer is derived from StopwordsAnalyzerBase.
 * 2. Similarity. Assumes DefaultSimilarity
 * 3. 
 */
public class LuceneBuildIndex {
    ClassLoader loader = ClassLoader.getSystemClassLoader();
    IndexConfig config;
    
    public LuceneBuildIndex(IndexConfig config) {
        this.config = config;
    }
    
    /**
     * Builds a Lucene index given a Yaml configuration file
     * @throws Exception
     */
    public void buildIndex() throws Exception {

        CorpusConfig corpusConfig = config.getCorpus();

        int numShards = corpusConfig.getShards();
    
        String filter = corpusConfig.getFilter();
        String corpusPath = corpusConfig.getPath();
        File corpusFile = new File(corpusPath);
        
        // Top level list of files to be processed
        List<File> files = new ArrayList<File>();
        if (!StringUtils.isEmpty(filter)) {
            FileFilter fileFilter = new WildcardFileFilter(filter);
            File[] filtered = corpusFile.listFiles(fileFilter);
            for (File file: filtered)
                files.add(file);
        } else if (corpusFile.isDirectory()){
            File[] fs = corpusFile.listFiles();
            for (File file: fs)
                files.add(file);
        } else
            files.add(corpusFile);
        
        Collections.sort(files);
        
        int shardSize = files.size() / numShards;
        List<List<File>> shards = new LinkedList<List<File>>(); 
        for (int i = 0; i < files.size(); i += shardSize) {
            shards.add(files.subList(i, i + Math.min(shardSize, files.size() - i)));
        }

        List<Thread> threads = new LinkedList<Thread>();
        int i= 0;
        for (List<File> shardFiles: shards) {
            Runnable task = new IndexerThread(String.valueOf(i), shardFiles);
            Thread worker = new Thread(task);
            worker.setName(String.valueOf(i));
            worker.start();
            threads.add(worker);
            i++;
        }        
        

        
        for (Thread thread: threads) {
            thread.join();
        }
    }
    

    public static void main(String[] args) throws Exception {
        
        if (args.length == 0) {
            System.err.println("You must specify a configuration file.");
            System.exit(-1);            
        }
        
        File yamlFile = new File(args[0]);
        if(!yamlFile.exists()) {
            System.err.println("File not found");
            System.exit(-1);
        }
        
        Yaml yaml = new Yaml(new Constructor(IndexConfig.class));
        IndexConfig config = (IndexConfig)yaml.load(new FileInputStream(yamlFile));

        LuceneBuildIndex builder = new LuceneBuildIndex(config);
        builder.buildIndex();
    }
    
    private class IndexerThread implements Runnable {
        
        List<File> files;
        String id;
        
        public IndexerThread(String id, List<File> files)
        {
            this.id = id;
            this.files = files;
        }
         
        public void run() {
            
            System.out.println("Starting thread " + id);
            long count = 0;
            try
            {
                String indexPath = config.getIndexPath() + File.separator + "shard" + id;        
                Directory dir = FSDirectory.open(new File(indexPath));
    
                // Initialize the analyzer
                StopwordAnalyzerBase defaultAnalyzer;
                String stopwordsPath = config.getStopwords();
                String analyzerClass = config.getAnalyzer();
                if (!StringUtils.isEmpty(analyzerClass))
                {
                    @SuppressWarnings("rawtypes")
                    Class analyzerCls = loader.loadClass(analyzerClass);
            
                    if (!StringUtils.isEmpty(stopwordsPath))
                    {
                        @SuppressWarnings({ "rawtypes", "unchecked" })
                        java.lang.reflect.Constructor analyzerConst = analyzerCls.getConstructor(Version.class, Reader.class);
                        defaultAnalyzer = (StopwordAnalyzerBase)analyzerConst.newInstance(Indexer.VERSION, new FileReader(stopwordsPath));            
                    } else {
                        @SuppressWarnings({ "rawtypes", "unchecked" })
                        java.lang.reflect.Constructor analyzerConst = analyzerCls.getConstructor(Version.class);
                        defaultAnalyzer = (StopwordAnalyzerBase)analyzerConst.newInstance(Indexer.VERSION);            
                    }
                } else {
                    defaultAnalyzer = new StandardAnalyzer(Indexer.VERSION, new CharArraySet(Indexer.VERSION, 0, true));
                }
                
                // Assumes LM similarity, but can be changed via config file
                Similarity similarity = new LMDirichletSimilarity();
                String similarityClass = config.getSimilarity();
                if (!StringUtils.isEmpty(similarityClass))
                    similarity = (Similarity)loader.loadClass(similarityClass).newInstance();
                
                // Setup any per-field analyzers.
                Map<String, Analyzer> perFieldAnalyzers = new HashMap<String, Analyzer>();
                Set<FieldConfig> fields = config.getFields();
                for (FieldConfig field: fields) {
                    String fieldAnalyzerClass = field.getAnalyzer();            
                    String fieldType = field.getType();
    
                    if (!StringUtils.isEmpty(fieldAnalyzerClass)) {
                        // Use per-field analyzer, if present
                        @SuppressWarnings("rawtypes")
                        Class fieldAnalyzerCls = loader.loadClass(fieldAnalyzerClass);
                        Analyzer fieldAnalyzer = (Analyzer)fieldAnalyzerCls.newInstance();
                        perFieldAnalyzers.put(field.getName(), fieldAnalyzer);
                    }
                    else if (!StringUtils.isEmpty(fieldType)&& fieldType.equals(FieldConfig.TYPE_ID)) {
                        // If the field type is ID, default to KeywordAnalyzer.
                        Analyzer fieldAnalyzer = new KeywordAnalyzer();
                        perFieldAnalyzers.put(field.getName(), fieldAnalyzer);
                    }            
                }
                
                Analyzer analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, perFieldAnalyzers);
                IndexWriterConfig iwc = new IndexWriterConfig(Indexer.VERSION, analyzer);
                iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
                iwc.setRAMBufferSizeMB(256.0);
                iwc.setSimilarity(similarity);
                            
                IndexWriter writer = new IndexWriter(dir, iwc);
                CorpusConfig corpusConfig = config.getCorpus();
                String corpusType = corpusConfig.getType();
                    
                try
                {
                    Indexer indexer;
                    if (corpusType.equals(Indexer.FORMAT_WIKI)) {      
                        indexer = new WikiTextIndexer();
                    } else if (corpusType.equals(Indexer.FORMAT_TRECTEXT)){ 
                        indexer = new TrecTextIndexer();
                    } else if (corpusType.equals(Indexer.FORMAT_TIKA)) {
                        indexer = new TikaIndexer();                
                    } else if (corpusType.equals(Indexer.FORMAT_STREAMCORPUS)) {
                        indexer = new StreamCorpusIndexer();                
                    } else {
                        throw new Exception("Unsupported corpus type/format.");                
                    }
        
                    for (File file: files) {
                        count += indexer.buildIndex(writer,  fields, file);                
                    }
                    System.out.println("Indexed " + count + " files");
                
    
                    writeIndexMetadata(indexPath, config);
                } catch (Exception e) {
                    System.out.println("Fatal: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    writer.close();
                }   
            } catch (Exception e) {
                System.out.println("Fatal: " + e.getMessage());
                e.printStackTrace();                
            }
        }
        
        public void writeIndexMetadata(String indexPath, IndexConfig config) 
                throws IOException {
            // Need to hold on to a few pieces of information
            FileWriter metadataWriter = new FileWriter(indexPath + File.separator + "index.metadata");
            
            String analyzer = config.getAnalyzer();
            if (!StringUtils.isEmpty(analyzer))
                metadataWriter.write("analyzer=" + analyzer);
            String similarity = config.getSimilarity();
            if (!StringUtils.isEmpty(similarity))
                metadataWriter.write("similarity=" + similarity);
            metadataWriter.close();
        }

    }
}
