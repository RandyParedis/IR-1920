package edu.gslis.eval;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;

/**
 * Container for trec_eval style batch IR results.
 * @author mefron
 *
 */
public class RetrievalResults implements RunResults {

	public static final Pattern SPACE_PATTERN = Pattern.compile("\\s+", Pattern.DOTALL);
	
	private static final int QUERY_COLUMN = 0;
	private static final int DOCNO_COLUMN = 2;
	private static final int SCORE_COLUMN = 4;
	
	private static final long MAX_TO_READ = 100000000000000L;

	private Map<String,SearchHits> allResults;
	
	
	public RetrievalResults(String pathToResultsFile) {
		try {
			
			allResults = new HashMap<String,SearchHits>();
			
			
			
			List<String> lines = IOUtils.readLines(new FileReader(new File(pathToResultsFile)));
			Iterator<String> linesIt = lines.iterator();
			while(linesIt.hasNext()) {
				String line = linesIt.next();
				String[] toks = SPACE_PATTERN.split(line);
				String query = toks[QUERY_COLUMN];
				String docno = toks[DOCNO_COLUMN];
				double score = Double.parseDouble(toks[SCORE_COLUMN]);
				
				SearchHit result = new SearchHit();
				result.setQueryName(query);
				result.setDocno(docno);
				result.setScore(score);
				
				SearchHits hitsForQuery = null;
				if(! allResults.containsKey(query)) 
					hitsForQuery = new SearchHits();
				else
					hitsForQuery = allResults.get(query);
				
				if(hitsForQuery.size() <= MAX_TO_READ) 
					hitsForQuery.add(result);
				
				allResults.put(query, hitsForQuery);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public SearchHits getResultsForQuery(String queryTitle) {
		if(! allResults.containsKey(queryTitle))
			return null;
		return allResults.get(queryTitle);
	}
	
}
