package edu.gslis.docaccumulators;


public class PostingsAggregator {
	
	public Postings aggregate(int[] docIds) {
		Postings postings = new Postings();
		
		int previousDocId = docIds[0];
		int workingDocId  =  0;
		int count = 0;
		
		for(int i=0; i<docIds.length; i++) {
			workingDocId = docIds[i];
			if(workingDocId != previousDocId) {
				postings.addEntry(previousDocId, count);
				previousDocId = workingDocId;
				count = 1;
			} else {
				count++;
			}
		}
		// finish the tail of the array
		postings.addEntry(workingDocId, count);
		
		return postings;
	}
}
