package edu.gslis.queries;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


/**
 * Attempts to automatically detect query format (currently works with JSON and Indri formats only).
 * @author Garrick
 *
 */
public class GQueriesFactory {
	
	/**
	 * Reads a query file and returns the appropriate GQueries object. Defaults to JSON (but does attempt to confirm this).
	 * 
	 * @param	path
	 * @return 			a GQueries object with  the topics read into memory
	 */
	public static GQueries getGQueries(String path) {
		GQueries queries = new GQueriesJsonImpl();
		
        String[] pieces = path.split("\\.");
    	if (pieces.length > 1 && pieces[1].equals("json")) {
    		System.err.println("Detected JSON file.");
		} else {
        	try {
        		BufferedReader reader = new BufferedReader(new FileReader(path));
        		String firstLine = reader.readLine();
        		reader.close();
        		
        		if (firstLine.contains("<parameters>")) {
        			System.err.println("Detected Indri file.");
        			queries = new GQueriesIndriImpl();
        		} else if (firstLine.contains("{")) {
        			System.err.println("Detected JSON file.");
        		}
        	} catch (FileNotFoundException e) {
        		System.err.println("Query file not found at " + path);
        	} catch (IOException e) {
				System.err.println("Unable to read file at " + path);
				e.printStackTrace();
			}
        }
		queries.read(path);
		return queries;
	}

}
