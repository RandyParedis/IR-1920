package edu.gslis.filtering.threshold;


import edu.gslis.eval.Qrels;
import edu.gslis.searchhits.SearchHits;



public class ThresholdFinderDummy extends ThresholdFinder {

	
	@Override
	public void init(String queryName, SearchHits resultsForQuery, Qrels qrels) {
		threshold = Double.NEGATIVE_INFINITY;
	}
	


	
	
	
}
