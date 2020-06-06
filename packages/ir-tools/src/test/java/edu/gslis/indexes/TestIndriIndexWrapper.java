package edu.gslis.indexes;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;

public class TestIndriIndexWrapper {
    
    @BeforeClass
    public static void setUp() {
        try
        {
            // Extract trectest.tgz
            File tmpDir = new File("testdata/tmp");
            tmpDir.mkdirs();
            
            Process proc = Runtime.getRuntime().exec("tar xvfz testdata/trectest/trectest.tgz -C testdata/tmp");            
            proc.waitFor();
            proc.destroy();
            
            // Run IndriBuildIndex
            Process bi = Runtime.getRuntime().exec("/usr/local/bin/IndriBuildIndex testdata/build_params.trectest");             
            BufferedReader br = new BufferedReader(new InputStreamReader(bi.getInputStream()));
            String l;
            while ((l = br.readLine()) != null) {
                //System.out.println("\t" + l);
            }
            bi.destroy();

        } catch (Exception e) {
            fail(e.getMessage());
        }
        
    }
    
    @AfterClass
    public static void tearDown() throws IOException
    { 
        File indexDir = new File("testdata/trectest.indri");
        FileUtils.deleteDirectory(indexDir);

    }
    
    @Test
    public void testIndriWrapper() {
        IndexWrapper index = new IndexWrapperIndriImpl("testdata/trectest.indri");

        String docno = "FT911-1";
        int docid = index.getDocId(docno);
        
        FeatureVector fv = index.getDocVector(docid, null);
        assertTrue(fv.getFeatureCount() == 122);
        assertTrue(fv.getLength() == 214);
        assertTrue(fv.getFeatureWeight("the") == 11);
        
        
        assertTrue(index.docCount() == 1047);  
        assertTrue(index.docFreq("the") == 1019);
//        assertTrue(index.docLengthAvg() == ?);
        assertTrue(index.getDocLength(docid) == 217);
        assertTrue(index.termCount() == 454038);
        assertTrue(index.getMetadataValue(docno, "epoch").equals("910514"));
        
        SearchHits hits = index.runQuery("raf cranwell", 100);
        assertTrue(hits.size() == 2);
        assertTrue(hits.getHit(0).getDocno().equals("FT911-1"));
        assertTrue(hits.getHit(1).getDocno().equals("FT911-4057"));

    }
}
