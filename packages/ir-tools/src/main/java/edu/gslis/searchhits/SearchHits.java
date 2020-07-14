package edu.gslis.searchhits;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.gslis.utils.ScorableComparator;


public class SearchHits implements Iterable<SearchHit> {
	private List<SearchHit> hits;
	private Iterator<SearchHit> iter;
	

	public SearchHit getHit(int i) {
		if(hits.size() <= i)
			return new SearchHit();
		return hits.get(i);
	}
	
	public SearchHits() {
		hits = new LinkedList<SearchHit>();	
	}
	
	public SearchHits(List<SearchHit> hits) {
		this.hits = hits;
	}
	
	public void add(SearchHit hit) {
		hits.add(hit);
	}
	
	public SearchHit remove(int i) {
	    return hits.remove(i);
	}
	public void rank() {
		ScorableComparator comparator = new ScorableComparator(true);
		Collections.sort(hits, comparator);
		iter = hits.iterator();
	}
	
	public Iterator<SearchHit> iterator() {
		iter = hits.iterator();
		return iter;
	}
	
	public int size() {
		return hits.size();
	}
	
	public void crop(int size) {
	    if (hits.size() > size) {
	        hits = hits.subList(0, size);
	        iter = hits.iterator();
	    }
	}
	
	public List<SearchHit> hits() {
	    return hits;
	}
	
	public void setHits(List<SearchHit> hits) {
	    this.hits = hits;
	}
	
	/** 
	 * Port of Lemur method.
	 */
    public void logToPosterior() 
    {
        Iterator<SearchHit> it = hits.iterator();
        double K = 0;
        while(it.hasNext()) {
            SearchHit hit = it.next();
            if (K == 0 || hit.getScore() > K) {
                K = hit.getScore();
            }
        }
        K = -K;
        double sum = 0;
        it = hits.iterator();
        while(it.hasNext()) {
            SearchHit hit = it.next();  
            double score = hit.getScore();
            score = Math.exp(K + score);
            hit.setScore(score);
            sum += score;
        }
        
        it = hits.iterator();
        while(it.hasNext()) {
            SearchHit hit = it.next();  
            double score = hit.getScore() / sum;
            hit.setScore(score);
        }
    }
}
