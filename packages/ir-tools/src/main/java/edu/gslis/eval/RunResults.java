package edu.gslis.eval;

import edu.gslis.searchhits.SearchHits;

/**
 * Generic container for the ouput of a batch run.  Could be retrieval, filtering, etc.
 * @author mefron
 *
 */
public interface RunResults {

	public SearchHits getResultsForQuery(String queryTitle);
}
