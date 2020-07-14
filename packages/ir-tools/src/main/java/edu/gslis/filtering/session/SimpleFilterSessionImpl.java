package edu.gslis.filtering.session;

import java.util.Iterator;
import java.util.List;

import edu.gslis.docaccumulators.ResultAccumulator;
import edu.gslis.docaccumulators.ResultAccumulatorNew;
import edu.gslis.docscoring.Scorer;
import edu.gslis.eval.Qrels;
import edu.gslis.filtering.threshold.SimpleCutoffThresholdClassifier;
import edu.gslis.filtering.threshold.ThresholdClassifier;
import edu.gslis.filtering.threshold.ThresholdFinder;
import edu.gslis.filtering.threshold.ThresholdFinderParamSweep;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.UnscoredSearchHit;

public class SimpleFilterSessionImpl implements FilterSession {
	private IndexWrapper trainIndex;
	private IndexWrapper testIndex;
	private Qrels trainQrels;
	private ThresholdClassifier thresholder;
	private Scorer scorer;
	private ThresholdFinder optimizer;
	private GQuery query;
	
	public  SimpleFilterSessionImpl(GQuery query, 
			IndexWrapper trainIndex,
			IndexWrapper testIndex,
			Qrels trainQrels,
			Scorer scorer,
			ThresholdFinder optimizer) {
		
		this.query = query;
		this.trainIndex = trainIndex;
		this.trainQrels = trainQrels;
		this.testIndex  = testIndex;
		this.scorer = scorer;
		this.optimizer = optimizer;
	}



	public void train() {
		ResultAccumulatorNew accumulator = new ResultAccumulatorNew((IndexWrapperIndriImpl)trainIndex, 
				query.getFeatureVector(), query.getMetadata(NAME_OF_CONSTRAINT_FIELD));
		
		
		accumulator.accumulate();
		List<UnscoredSearchHit> trainingAccumulated = accumulator.getChronologicallyOrderedDocs();
		

		SearchHits trainingHits = new SearchHits();
		Iterator<UnscoredSearchHit> docIterator = trainingAccumulated.iterator();
		while(docIterator.hasNext()) {
			UnscoredSearchHit unscoredHit = docIterator.next();
			SearchHit hit = unscoredHit.toSearchHit();
			double score = scorer.score(hit);
			hit.setScore(score);
			trainingHits.add(hit); 
		}	
		
		
		optimizer.init(query.getTitle(), trainingHits, trainQrels);
		thresholder = new SimpleCutoffThresholdClassifier();
		((SimpleCutoffThresholdClassifier)thresholder).setThreshold(optimizer.getThreshold());
	}

	public SearchHits filter() {
		ResultAccumulatorNew accumulator = new ResultAccumulatorNew((IndexWrapperIndriImpl)testIndex, 
				query.getFeatureVector(), query.getMetadata(NAME_OF_CONSTRAINT_FIELD));
		accumulator.accumulate();
		List<UnscoredSearchHit> testingAccumulated = accumulator.getChronologicallyOrderedDocs();
		

		SearchHits testingEmitted = new SearchHits();
		Iterator<UnscoredSearchHit> docIterator = testingAccumulated.iterator();
		while(docIterator.hasNext()) {
			UnscoredSearchHit unscoredHit = docIterator.next();
			SearchHit hit = unscoredHit.toSearchHit();
			double score = scorer.score(hit);
			hit.setScore(score);
			
			if(Double.isInfinite(thresholder.getThreshold()) || thresholder.emit(score))
				testingEmitted.add(hit); 
			
			
		}	
		
		System.err.println("accumulated: " + testingAccumulated.size() + 
				"   emitted: " + testingEmitted.size() + " cutoff: " +
				thresholder.getThreshold());
		
		return testingEmitted;
	}

}
