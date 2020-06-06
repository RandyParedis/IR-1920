package edu.gslis.searchhits;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.textrepresentation.FeatureVector;



public class UnscoredSearchHit {
	
	private double length = 0.0;
	private int docID = 0;
	private String docno;
	private double epoch = 0.0;
	private FeatureVector lexicalFeatures;
	private Map<String,Object> metadata;
	
	public UnscoredSearchHit(String docno, int docID, double length, double epoch) {
		lexicalFeatures = new FeatureVector(null);
		metadata        = new HashMap<String,Object>();
		
		this.docno  = docno; 
		this.docID  = docID;
		this.length = length;
		this.epoch  = epoch;
	}
	
	public void addFeature(String key, double value) {
		lexicalFeatures.addTerm(key, value);
	}
	
	public double getValue(String key) {
		return lexicalFeatures.getFeatureWeight(key);
	}
	
	public Iterator<String> iterator() {
		return lexicalFeatures.iterator();
	}
	public void setMetadata(String key, Object value) {
		metadata.put(key, value);
	}
	public Object getMetadata(String key) {
		if(!metadata.containsKey(key)) {
			System.err.println("can't find metadata for key:<" + key + "> in UnscoredSearchHit.");
			System.exit(-1);
		}
		return metadata.get(key);
	}
	public String getDocno() {
		return docno;
	}
	public double getLength() {
		return length;
	}
	public int getDocID() {
		return docID;
	}
	public double getEpoch() {
		return epoch;
	}
	
	/**
	 * TODO: horribly inefficient!! need to integrate with SearchHit class.
	 * @return
	 */
	public SearchHit toSearchHit() {
		SearchHit hit = new SearchHit();
		hit.setFeatureVector(lexicalFeatures);
		hit.setDocID(getDocID());
		hit.setDocno(getDocno());
		hit.setLength(getLength());
		hit.setMetadataValue("epoch", getEpoch());
		return hit;
	}
	
	public FeatureVector toDocVector() {
		return lexicalFeatures;
	}
	
	@Override
	public String toString() {
		return lexicalFeatures.toString();
	}

}
