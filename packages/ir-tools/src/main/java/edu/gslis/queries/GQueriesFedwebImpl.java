package edu.gslis.queries;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;

import edu.gslis.textrepresentation.FeatureVector;



/**
 * Reads a set of queries formatted for Fedweb 2013 or Fedweb 2014
 */
public class GQueriesFedwebImpl extends GQueries {

	
	
	public void read(String pathToQueries) {

	    try
	    {
            List<String> rows = FileUtils.readLines(new File(pathToQueries));
            for (String row: rows) {
                String[] fields = null;
                if (row.contains(":")) {
                    fields = row.split(":");
                } else if (row.contains("\t")) {
                    fields = row.split("\t");
                }
                
                FeatureVector fv = new FeatureVector(fields[1], null);
                GQuery query = new GQuery();
                query.setTitle(fields[0]);
                query.setText(fields[1]);
                query.setFeatureVector(fv);
                
                addQuery(query);            
            }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

}
