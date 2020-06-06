package edu.gslis.filtering.threshold;

import java.util.Arrays;
import java.util.Iterator;


import edu.gslis.eval.FilterEvaluation;
import edu.gslis.eval.Qrels;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;



public class ThresholdFinderParamSweep extends ThresholdFinder{
	public static final String STAT_TO_OPTIMIZE = "f1";
	public static final double DELTA = 0.05;
	private double threshold = Double.NEGATIVE_INFINITY;
	private double lowerBound = Double.NEGATIVE_INFINITY;
	private double upperBound = Double.POSITIVE_INFINITY;
	
	private String queryName;
	private SearchHits resultsForQuery;
	private FilterEvaluation evaluation;
	private double bestObservedPerformance = 0.0;
	
	

	public void init(String queryName, SearchHits resultsForQuery, Qrels qrels) {
		this.queryName = queryName;
		this.resultsForQuery = resultsForQuery;
		this.evaluation = new FilterEvaluation(qrels);
		evaluation.setResults(resultsForQuery);
	}
	
	@Override
	public double getThreshold() {
		optimize();
		return threshold;
	}

	
	private void optimize() {
		lowerBound = this.minOrMax(resultsForQuery, true);
		upperBound = this.minOrMax(resultsForQuery, false);
		threshold = optimizeF(lowerBound, upperBound);		
	}
	
	private double optimizeF(double lowerBound, double upperBound) {
		double bestScore = Double.NEGATIVE_INFINITY;
		double bestThreshold = lowerBound;
		double workingThreshold = lowerBound;
		while(workingThreshold < upperBound) {
			SearchHits culledHits = cullHits(resultsForQuery, workingThreshold);
			evaluation.setResults(culledHits);
			double observedScore = evaluation.f1Query(queryName);
			if(observedScore > bestScore) {
				bestScore = observedScore;
				bestThreshold = workingThreshold;
			}
			workingThreshold += DELTA;
		}
		bestObservedPerformance = bestScore;
		return bestThreshold;
	}
	
	private SearchHits cullHits(SearchHits hits, double cutoff) {
		SearchHits culledHits = new SearchHits();
		Iterator<SearchHit> hitIterator = hits.iterator();
		while(hitIterator.hasNext()) {
			SearchHit hit = hitIterator.next();
			if(hit.getScore() >= cutoff) 
				culledHits.add(hit);
		}
		return culledHits;
	}
	
	

	
	private double[] extractRsvs(SearchHits hits) {
		double[] rsvs = new double[hits.size()];
		int k=0;
		Iterator<SearchHit> hitIterator = hits.iterator();
		while(hitIterator.hasNext()) {
			SearchHit hit = hitIterator.next();
			rsvs[k++] = hit.getScore();
		}
		return rsvs;
	}
	
	private double minOrMax(SearchHits hits, boolean getMin) {
		double[] rsvs = this.extractRsvs(hits);
		Arrays.sort(rsvs);
		if(rsvs.length<1)
			return Double.NEGATIVE_INFINITY;
		if(getMin)
			return rsvs[0];
		return rsvs[rsvs.length-1];
	}
	



	
	public double getBestPerformance() {
		return bestObservedPerformance;
	}

	
}
