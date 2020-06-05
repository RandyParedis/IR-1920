package edu.gslis.searchhits;

import edu.gslis.indexes.IndexWrapper;

public class IndexBackedSearchHits {
	
	public static SearchHits convertToIndexBackedSearchHits(SearchHits nonIndexBackedSearchHits, IndexWrapper index) {
		SearchHits newHits = new SearchHits();
		for (int i = 0; i < nonIndexBackedSearchHits.size(); i++) {
			SearchHit indexBacked = new IndexBackedSearchHit(index, nonIndexBackedSearchHits.getHit(i));
			newHits.add(indexBacked);
		}
		return newHits;
	}

}
