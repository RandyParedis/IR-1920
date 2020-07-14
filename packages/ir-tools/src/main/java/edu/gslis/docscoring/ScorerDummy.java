package edu.gslis.docscoring;


import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;

/**
 * 
 * 
 * @author mefron
 *
 */
public class ScorerDummy extends QueryDocScorer {


	
	public void setQuery(GQuery query) {
		this.gQuery = query;
	}
	

	public double score(SearchHit doc) {
		return Double.POSITIVE_INFINITY;
	}
	

	
	


}
