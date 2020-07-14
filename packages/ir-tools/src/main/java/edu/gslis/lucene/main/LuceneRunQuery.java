package edu.gslis.lucene.main;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import edu.gslis.lucene.indexer.Indexer;
import edu.gslis.lucene.main.config.QueryConfig;
import edu.gslis.lucene.main.config.QueryFile;
import edu.gslis.lucene.main.config.RunQueryConfig;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesFedwebImpl;
import edu.gslis.queries.GQueriesIndriImpl;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.textrepresentation.FeatureVector;


public class LuceneRunQuery {
    ClassLoader loader = ClassLoader.getSystemClassLoader();
    RunQueryConfig config;
    
    public LuceneRunQuery(RunQueryConfig config) {
        this.config = config;
    }
    
    public void run() throws Exception {
        String indexPath = config.getIndex();
        String docnoField = config.getDocno();
        if (StringUtils.isEmpty(docnoField))
            docnoField = "docno";
        String similarityClass = config.getSimilarity();
        String stopwordsPath = config.getStopwords();
        String analyzerClass = config.getAnalyzer();
        StopwordAnalyzerBase defaultAnalyzer;
        
        Map<String, String> indexMetadata = readIndexMetadata(indexPath);
        if (StringUtils.isEmpty(analyzerClass) && indexMetadata.get("analyzer") != null) 
            analyzerClass = indexMetadata.get("analyzer");
        if (StringUtils.isEmpty(similarityClass) && indexMetadata.get("similarity") != null) 
            similarityClass = indexMetadata.get("similarity");
                                
        if (!StringUtils.isEmpty(analyzerClass))
        {
            @SuppressWarnings("rawtypes")
            Class analyzerCls = loader.loadClass(analyzerClass);

            if (!StringUtils.isEmpty(stopwordsPath))
            {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                java.lang.reflect.Constructor analyzerConst = analyzerCls.getConstructor(Version.class, Reader.class);
                defaultAnalyzer = (StopwordAnalyzerBase)analyzerConst.newInstance(new FileReader(stopwordsPath));
            } else {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                java.lang.reflect.Constructor analyzerConst = analyzerCls.getConstructor(Version.class);
                defaultAnalyzer = (StopwordAnalyzerBase)analyzerConst.newInstance();
            }
        } else {
            defaultAnalyzer = new StandardAnalyzer(new CharArraySet(0, true));
        }



        // Assumes LM similarity, but can be changed via config file
        Similarity similarity = new LMDirichletSimilarity();
        if (!StringUtils.isEmpty(similarityClass))
            similarity = (Similarity)loader.loadClass(similarityClass).newInstance();
        IndexReader reader = DirectoryReader.open(FSDirectory.open((new File(indexPath)).toPath()));
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(similarity);
        String field = config.getField();
        if (StringUtils.isEmpty(field))
            field = "text";
        String[] fields = field.split(",");
 

         System.err.println("Similarity: " + similarityClass);
         System.err.println("Analyzer: " + analyzerClass);


        Set<QueryConfig> queries = config.getQueries();        
        if (queries == null)
            queries = new HashSet<QueryConfig>(); 
        QueryFile queryFile = config.getQueryFile();
        if (queryFile != null) {
            String path = queryFile.getPath();
            String format = queryFile.getFormat();
            
            queries = readQueries(path, format);
        }
        
        for (QueryConfig query: queries) {
            QueryParser parser = new MultiFieldQueryParser(fields, defaultAnalyzer);
            Query q = parser.parse(query.getText());
            TopDocs topDocs = searcher.search(q, 1000);
            ScoreDoc[] docs = topDocs.scoreDocs;
            Set<String> seen = new HashSet<String>();
            for (int i=0; i<docs.length; i++) {
                int docid = docs[i].doc;
                double score = docs[i].score;
                Document doc = searcher.doc(docid);
                
                String docno = doc.getField(docnoField).stringValue();
                long doclen = doc.getField(Indexer.FIELD_DOC_LEN).numericValue().longValue();

                if (!seen.contains(docno)) {
                    //http://en.wikipedia.org/wiki/Shafi_Goldwasser Q0 1325799600-2efdd8d6cd5b665a3a61a1faeb0c3410 161 -32.654207071883675 plm
                    System.out.println(query.getNumber() + " Q0 " + docno + " " + i + " "  + score + " gslis");
                    seen.add(docno);
                }
            }

        }
    }
    
    public static void main(String[] args) throws Exception {
        RunQueryConfig config = new RunQueryConfig();
        if (args.length == 0) 
        {
            System.err.println("you must specify a configuration file.");
            System.exit(-1);
        }
        else if (args.length == 1) {
            File yamlFile = new File(args[0]);
            if(!yamlFile.exists()) {
                System.err.println("Configuration file not found.");
                System.exit(-1);
            }
            Yaml yaml = new Yaml(new Constructor(RunQueryConfig.class));
            config = (RunQueryConfig)yaml.load(new FileInputStream(yamlFile));
        }        
        else {
            Options options = createOptions();
            CommandLineParser parser = new GnuParser();
            CommandLine cmd = parser.parse( options, args);
            
            String analyzer = cmd.getOptionValue("analyzer", Indexer.DEFAULT_ANALYZER);
            String docno = cmd.getOptionValue("docno", Indexer.FIELD_DOCNO);
            String field = cmd.getOptionValue("field", Indexer.FIELD_TEXT);
            String querynum = cmd.getOptionValue("querynum", "1");
            String runname = cmd.getOptionValue("name", "gslis");
            
            String similarity = cmd.getOptionValue("similarity", Indexer.DEFAULT_SIMILARITY);

            String query = cmd.getOptionValue("query");
            String queryfile = cmd.getOptionValue("queryfile");
            String format = cmd.getOptionValue("format");
            String index = cmd.getOptionValue("index");
            String stopwords = cmd.getOptionValue("stopwords");
            
            Set<QueryConfig> queries = new HashSet<QueryConfig>();                    
            if (!StringUtils.isEmpty(query)) {
                QueryConfig querycfg = new QueryConfig();
                querycfg.setNumber(querynum);
                querycfg.setText(query.replaceAll("'", "\""));
                queries.add(querycfg);
            } else if (!StringUtils.isEmpty(queryfile)) {
                queries = readQueries(queryfile, format);
            }
            
            config.setAnalyzer(analyzer);
            config.setField(field);
            config.setIndex(index);
            config.setQueries(queries);
            config.setDocno(docno);
            config.setSimilarity(similarity);
            config.setStopwords(stopwords);
        }            
        LuceneRunQuery runner = new LuceneRunQuery(config);
        runner.run();
    }
    
    public Map<String, String> readIndexMetadata(String indexPath) 
    {
        Map<String, String> map = new HashMap<String, String>();
        try
        {
            File metadata = new File(indexPath + File.separator + "index.metadata");
            List<String> lines = FileUtils.readLines(metadata);
            for (String line: lines) {
                String[] fields = line.split("=");
                map.put(fields[0], fields[1]);            
            }
        } catch (IOException e) {
            // Can't find the index.metadata file, use overrides
        }
        return map;
    }
    
    public static Set<QueryConfig> readQueries(String file, String format) throws Exception 
    {
        Set<QueryConfig> queries = new TreeSet<QueryConfig>();

        GQueries gqueries = null;
        if (format.equals("fedweb")) {
           gqueries = new GQueriesFedwebImpl();
        }
        else if (format.equals("json")) {
            gqueries = new GQueriesJsonImpl();
        }
        else if (format.equals("indri")) {
            gqueries = new GQueriesIndriImpl();
        }
        
        gqueries.read(file);


        Iterator<GQuery> it = gqueries.iterator();
        while(it.hasNext()) {
            GQuery query = it.next();
            
            StringBuffer qstr = new StringBuffer();
            FeatureVector fv = query.getFeatureVector();
            Iterator<String> fit = fv.iterator();
            while (fit.hasNext()) {
                String feature = fit.next();
                double weight = fv.getFeatureWeight(feature);
                qstr.append(feature + "^" + weight);
                if (fit.hasNext()) 
                    qstr.append(" ");
                
            }
            
            QueryConfig qc = new QueryConfig();
            qc.setText(qstr.toString());
            qc.setNumber(query.getTitle());
            queries.add(qc);
        }
        
        return queries;
    }
    
    
    public static Options createOptions()
    {
        Options options = new Options();
        options.addOption("analyzer", true, "Analyzer class");
        options.addOption("index", true, "Path to index");
        options.addOption("field", true, "Field to search");
        options.addOption("docno", true, "Docno field");
        options.addOption("querynum", true, "Query identifier");
        options.addOption("query", true, "Query string");
        options.addOption("queryfile", true, "Query file");
        options.addOption("format", true, "Query file format");
        options.addOption("similarity", true, "Similarity class");
        options.addOption("stopwords", true, "Stopwords list");
        options.addOption("name", true, "Run name");
        return options;
    }

}
