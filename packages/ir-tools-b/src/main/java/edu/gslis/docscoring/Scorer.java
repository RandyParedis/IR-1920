package edu.gslis.docscoring;

import edu.gslis.searchhits.SearchHit;


/**
 * Generic behavior for deriving a score for a doc.  can be query-dependent or query-independent.
 * 
 * @author mefron
 *
 */
public interface Scorer {

	public double score(SearchHit doc);
	
	public void setParameter(String paramName, double paramValue);
	
}
