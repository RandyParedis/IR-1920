package edu.gslis.indexes;

import java.io.File;


/**
 * Attempts to auto-dected index type.
 * @author cwillis
 */
public class IndexWrapperFactory {

    public static IndexWrapper getIndexWrapper(String path) 
    {
        File segments = new File(path + File.separator + "segments.gen");
        File manifest = new File(path + File.separator + "manifest");
        
        if (segments.exists()) {
            return new IndexWrapperLuceneImpl(path);
        }
        else if (manifest.exists()) {
            return new IndexWrapperIndriImpl(path);
        }
        else
            return null;
    }

}
