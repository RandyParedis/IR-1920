package edu.gslis.utils.main;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;

import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;


public class IndriQueriesToGqueries {
	
	public static String getGQueries(String pathToIndriQueries, Stopper stopper) {
		GQueries gQueries = new GQueriesJsonImpl();
		
		List<String> lines = null;
		try {
			lines = IOUtils.readLines(new FileReader(pathToIndriQueries));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Iterator<String> lineIterator = lines.iterator();
		while(lineIterator.hasNext()) {
			String line = lineIterator.next();
			if(line.contains("<number>")) {
				String title = line;
				title = title.replaceAll("<.?number>", "").trim();
				lineIterator.next(); // <text>
				String text = lineIterator.next();
				GQuery gQuery = new GQuery();
				gQuery.setTitle(title);
				gQuery.setText(text);
				gQuery.setFeatureVector(new FeatureVector(text, stopper));
				gQueries.addQuery(gQuery);
			}
		}
		
		return gQueries.toString();
	}
	
	
	public static void main(String[] args) {
		String pathToIndriQueries = args[0];
		Stopper stopper = null;
		if(args.length > 1) {
			stopper = new Stopper(args[1]);
		}
		System.out.println(IndriQueriesToGqueries.getGQueries(pathToIndriQueries, stopper));
		

			
	}

}
