package edu.gslis.searchhits;

import java.util.HashMap;
import java.util.Map;

import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Scorable;




public class SearchHit implements Scorable, Comparable<SearchHit> {
	
	private String queryName;
	private String docno;
	private int docID;
	private double score;
	private double length;
	private FeatureVector docVector;		// e.g. a term vector representing this doc
	private Map<String,Object> metadata;	// e.g. info such as a unix epoch
	
	public SearchHit() {
		metadata = new HashMap<String,Object>();
	}
	
	
	public Object getMetadataValue(String property) {
		if(!metadata.containsKey(property)) {
			System.err.println("requested non-existent metadata " + property + " from doc " + docno);
			System.exit(-1); 
		} 
		return metadata.get(property);
	}
	public void setMetadataValue(String property, Object value) {
		metadata.put(property, value);
	}
	
	public String getQueryName() {
		return queryName;
	}
	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}
	public String getDocno() {
		return docno;
	}
	public void setDocno(String docno) {
		this.docno = docno;
	}
	public int getDocID() {
		return docID;
	}
	public void setDocID(int docID) {
		this.docID = docID;
	}
	public void setFeatureVector(FeatureVector docVector) {
		this.docVector = docVector;
	}
	public FeatureVector getFeatureVector() {
		return docVector;
	}
	public void setLength(double length) {
		this.length = length;
	}
	public double getLength() {
		return length;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public double getScore() {
		return score;
	}
	
    public int compareTo(SearchHit h) {
        if (this.score != h.score)
            return Double.compare(this.score, h.score);
        else    
            return docno.compareTo(h.docno);
    }
    
    @Override
    public String toString() {
    	return "Docno: " + getDocno() + "; DocID: " + getDocID(); 
    }
    
    @Override
    public boolean equals(Object obj) {
    	if (obj == null || !(obj instanceof SearchHit)) {
    		return false;
    	}
    	SearchHit otherHit = (SearchHit) obj;
    	return (otherHit.getDocID() == getDocID()
    			&& otherHit.getDocno().equals(getDocno()));
    }
    
    @Override
    public int hashCode() {
    	if (getDocno() != null) {
			return getDocno().hashCode();
    	}
    	return getDocID();
    }
    
}
