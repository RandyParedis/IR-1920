package edu.gslis.docscoring.support;


public abstract class CollectionStats {
	protected double tokCount;
	protected double docCount;
	protected double termTypeCount;
	
	public abstract double termCount(String term);
	public abstract double docCount(String term);
	
	public abstract void setStatSource(String statSource);
	
	public double getTokCount() {
		return tokCount;
	}
	
	public double getDocCount() {
		return docCount;
	}
	
	public double getTermTypeCount() {
	    return termTypeCount;
	}
	
}
