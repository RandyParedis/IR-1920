package edu.gslis.indexes;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import edu.gslis.lucene.main.LuceneBuildIndex;
import edu.gslis.lucene.main.config.IndexConfig;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;

public class TestLuceneIndexWrapper {

    @BeforeClass
    public static void setUp() {
        try
        {
            // Index trectest.tgz
            String yamlFile = "testdata/build_index_trectest.yml";
            Yaml yaml = new Yaml(new Constructor(IndexConfig.class));
            IndexConfig config = (IndexConfig)yaml.load(new FileInputStream(yamlFile));
    
            LuceneBuildIndex builder = new LuceneBuildIndex(config);
            builder.buildIndex();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        
    }
    
    @AfterClass
    public static void tearDown() throws IOException {
        File indexDir = new File("testdata/trectest.lucene");
        FileUtils.deleteDirectory(indexDir);
    }
    @Test
    public void testLuceneWrapper() {
        IndexWrapper index = new IndexWrapperLuceneImpl("testdata/trectest.lucene");

        String docno = "FT911-1";
        int docid = index.getDocId(docno);
        
        FeatureVector fv = index.getDocVector(docid, null);

       // System.out.println(fv.toString());
        assertTrue(fv.getFeatureCount() == 121);
        assertTrue(fv.getLength() == 213);
        assertTrue(fv.getFeatureWeight("the") == 11);
                
        assertTrue(index.docCount() == 1047);          
        assertTrue(index.docFreq("the") == 1242);
//        assertTrue(index.docLengthAvg() == ?);
        assertTrue(index.getDocLength(docid) == 213);
        assertTrue(index.termCount() == 446812); //446812
        assertTrue(index.getMetadataValue(docno, "epoch").equals("910514"));
        
        SearchHits hits = index.runQuery("raf cranwell", 100);
        assertTrue(hits.size() == 2);
        assertTrue(hits.getHit(0).getDocno().equals("FT911-1"));
        assertTrue(hits.getHit(1).getDocno().equals("FT911-4057"));        
    }
}
