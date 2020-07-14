package edu.gslis.docscoring;

import edu.gslis.searchhits.SearchHit;


/**
 * Simply return the underlying retrieval score
 */
public class ScorerNull extends QueryDocScorer {

	public double score(SearchHit doc) {
		return doc.getScore();
	}

}
