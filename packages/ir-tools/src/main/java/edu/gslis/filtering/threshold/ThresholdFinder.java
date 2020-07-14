package edu.gslis.filtering.threshold;

import edu.gslis.eval.Qrels;
import edu.gslis.searchhits.SearchHits;

public abstract class ThresholdFinder {
	protected double threshold = Double.NEGATIVE_INFINITY;
	
	public abstract void init(String queryName, SearchHits resultsForQuery, Qrels qrels);
	
	public double getThreshold() {
		return threshold;
	}
}
