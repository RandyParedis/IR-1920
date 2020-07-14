package edu.gslis.docaccumulators;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Postings {
	private Map<Integer,Integer> postings;	// docId -> count
	
	
	public Postings() {
		postings = new HashMap<Integer,Integer>();
	}
	
	public void addEntry(int docId, int count) {
		postings.put(docId, count);
	}
	
	public int lookup(int docId) {
		return (postings.containsKey(docId)) ? postings.get(docId) : 0;
	}
	
	public Iterator<Integer> docIdIterator() {
		return postings.keySet().iterator();
	}
	
	public String toString() {
		StringBuilder b = new StringBuilder();
		Iterator<Integer> docIdIterator = postings.keySet().iterator();
		while(docIdIterator.hasNext()) {
			int docId = docIdIterator.next();
			int count = postings.get(docId);
			b.append("doc " + docId + "\t -> " + count + System.getProperty("line.separator"));
		}
		return b.toString();
	}
}
