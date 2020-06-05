package edu.gslis.searchhits;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHits;

public class SearchHitsBatch {
	
	Map<String, SearchHits> queryToSearchHits;
	
	public SearchHitsBatch() {
		queryToSearchHits = new HashMap<String, SearchHits>();
	}
	
	public Iterator<String> queryIterator() {
		return queryToSearchHits.keySet().iterator();
	}
	
	public Iterator<SearchHits> searchHitIterator() {
		return queryToSearchHits.values().iterator();
	}
	
	public SearchHits getSearchHits(GQuery query) {
		return getSearchHits(query.getTitle());
	}

	public SearchHits getSearchHits(String query) {
		return queryToSearchHits.get(query);
	}
	
	public void setSearchHits(GQuery query, SearchHits hits) {
		setSearchHits(query.getTitle(), hits);
	}
	
	public void setSearchHits(String query, SearchHits hits) {
		queryToSearchHits.put(query, hits);
	}
	
	public int getNumQueries() {
		return queryToSearchHits.keySet().size();
	}
	
	public void addBatchResults(SearchHitsBatch batchResults) {
		Iterator<String> qit = batchResults.queryIterator();
		while (qit.hasNext()) {
			String query = qit.next();
			queryToSearchHits.put(query, batchResults.getSearchHits(query));
		}
	}

}
