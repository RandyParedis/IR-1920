package edu.gslis.queries.expansion;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.KeyValuePair;
import edu.gslis.utils.KeyValuePairs;
import edu.gslis.utils.Stopper;



public abstract class Feedback {
	protected IndexWrapper index;
	protected SearchHits relDocs;
	protected SearchHits nrelDocs;
	protected GQuery originalQuery;
	protected int fbDocCount  = 20;
	protected int fbTermCount = 20;
	protected KeyValuePairs features;		// these will be KeyValuePair objects
	protected Stopper stopper;
	
	
	
	public abstract void build();
	

	
	public GQuery asGquery() {
		GQuery newQuery = new GQuery();
		newQuery.setTitle(originalQuery.getTitle());
		newQuery.setText(originalQuery.getText());
		
		FeatureVector finalVector = new FeatureVector(stopper);
		
		features.sort(true);
		Iterator<KeyValuePair> it = features.iterator();
		
		int i=0;
		while(it.hasNext() && i++ < fbTermCount) {			
			KeyValuePair tuple = it.next();
			finalVector.addTerm(tuple.getKey(), tuple.getScore());
		}
		
		newQuery.setFeatureVector(finalVector);
		
		return newQuery;
	}

	public FeatureVector asFeatureVector() {
		FeatureVector f = new FeatureVector(stopper);
		Iterator<KeyValuePair> it = features.iterator();
		
		while(it.hasNext()) {			
			KeyValuePair tuple = it.next();
			f.addTerm(tuple.getKey(), tuple.getScore());
		}	
		
		return f;
	}
	
	public Map<String,Double> asMap() {
		Map<String,Double> map = new HashMap<String,Double>(features.size());
		Iterator<KeyValuePair> it = features.iterator();
		while(it.hasNext()) {
			KeyValuePair tuple = it.next();
			map.put(tuple.getKey(), tuple.getScore());
		}

		return map;
	}
	
	public void setDocCount(int fbDocCount) {
		this.fbDocCount = fbDocCount;
	}
	public void setTermCount(int fbTermCount) {
		this.fbTermCount = fbTermCount;
	}
	
	@Override 
	public String toString() {
		return toString(features.size());
	}
	
	public String toString(int k) {
		DecimalFormat format = new DecimalFormat("#.#####################");
		features.sort(true);
		double sum = 0.0;
		Iterator<KeyValuePair> it = features.iterator();
		int i=0;
		while(it.hasNext() && i++ < k) {			
			sum += it.next().getScore();
		}
		
		StringBuilder b = new StringBuilder();
		it = features.iterator();
		i=0;
		while(it.hasNext() && i++ < k) {			
			KeyValuePair tuple = it.next();
			b.append(format.format(tuple.getScore()/sum) + " " + tuple.getKey() + "\n");
		}
		
		return b.toString();
	}
	
	

	public void setIndex(IndexWrapper index) {
		this.index = index;
	}
	public void setRes(SearchHits relDocs) {
		this.relDocs = relDocs;
	}
	public void setNRelDocs(SearchHits nrelDocs) {
	    this.nrelDocs = nrelDocs;
	}
	public void setOriginalQuery(GQuery originalQuery) {
		this.originalQuery = originalQuery;
	}
	public void setStopper(Stopper stopper) {
		this.stopper = stopper;
	}
	
	
	
	
}
