package edu.gslis.filtering.session;

import edu.gslis.searchhits.SearchHits;

public interface FilterSession {
	public static final String NAME_OF_EMIT_STATUS_FIELD = "emit";
	public static final String NAME_OF_CONSTRAINT_FIELD  = "constraint";
    public static final String NAME_OF_TIMESTAMP_FIELD  = "timestamp";
	
	public void train();
	
	public SearchHits filter();
	
}
