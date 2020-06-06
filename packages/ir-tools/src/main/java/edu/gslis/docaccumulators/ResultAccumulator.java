package edu.gslis.docaccumulators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lemurproject.indri.QueryEnvironment;
import lemurproject.indri.ScoredExtentResult;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.searchhits.UnscoredSearchHit;
import edu.gslis.textrepresentation.FeatureVector;

public class ResultAccumulator {

	private FeatureVector queryModel;
	private QueryEnvironment env;
	private Map<Integer,UnscoredSearchHit> accumulatedFilteredDocs;
	private String constraint;
	
	// inner class for sorting hits on time
	private class RealTimeDocChronologicalComparator implements Comparator<UnscoredSearchHit>{
		private boolean decreasing = true;

		public RealTimeDocChronologicalComparator(boolean decreasing) {
			this.decreasing = decreasing;
		}
		public int compare(UnscoredSearchHit x, UnscoredSearchHit y) {
			double xVal = x.getEpoch();
			double yVal = y.getEpoch();

			if(decreasing) {
				return (xVal > yVal  ? -1 : (xVal == yVal ? 0 : 1));
			} else {
				return (xVal < yVal  ? -1 : (xVal == yVal ? 0 : 1));
			}
		}	
	}

	public ResultAccumulator(IndexWrapperIndriImpl indexWrapper, FeatureVector queryModel, String constraint) {
		
		// danger!  assumes we've got an indri index
		this.env = (QueryEnvironment)indexWrapper.getActualIndex();
		this.queryModel = queryModel;
		this.constraint = constraint;
		accumulatedFilteredDocs = new HashMap<Integer,UnscoredSearchHit>();
	}



	public void accumulate() {
		try {



			// accumulate all possibly relevant docs
			ScoredExtentResult[] allResults = env.expressionList(constraint);
			
			
			if(allResults.length==0)
				return;
			String[] docnos   = env.documentMetadata(allResults, "docno");
			String[] epochs   = env.documentMetadata(allResults, "epoch");


			int k=0;
			for(ScoredExtentResult r: allResults) {
				int docID = r.document;
				String docno = docnos[k];
				double length = (double)env.documentLength(docID);
				double epoch = Double.parseDouble(epochs[k]);
				UnscoredSearchHit hit = new UnscoredSearchHit(docno, docID, length, epoch);
				accumulatedFilteredDocs.put(docID, hit);
				k++;
			}

			// now iterate over each query term
			Iterator<String> featureIterator = queryModel.iterator();
			while(featureIterator.hasNext()) {
				String feature = featureIterator.next();
				
				
				String compoundQuery = "#band(" + constraint + " #band(" + feature + "))";
				
				
				ScoredExtentResult[] featureResults = env.expressionList(compoundQuery);

				if(featureResults.length==0)
					continue;
				// convert expression list to term-doc counts
				int[] docIds = this.extractDocIds(featureResults);
				PostingsAggregator postingsAggregator = new PostingsAggregator();
				Postings postingsForFeature = postingsAggregator.aggregate(docIds);
				Iterator<Integer> matchingDocIdIterator = postingsForFeature.docIdIterator();
				while(matchingDocIdIterator.hasNext()) {
					int docId = matchingDocIdIterator.next();
					int count = postingsForFeature.lookup(docId);
					
					UnscoredSearchHit hit = accumulatedFilteredDocs.get(docId);
					if(hit == null) {
						continue;
					}
					
					hit.addFeature(feature, count);
					accumulatedFilteredDocs.put(docId, hit);
				}
			}



		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private int[] extractDocIds(ScoredExtentResult[] r) {
		int[] d = new int[r.length];
		for(int i=0; i<r.length; i++) {
			d[i] = r[i].document;
		}
		return d;
	}

	public Map<Integer,UnscoredSearchHit> getAccumulatedDocs() {
		return accumulatedFilteredDocs;
	}

	public List<UnscoredSearchHit> getChronologicallyOrderedDocs() {
		List<UnscoredSearchHit> docsToProcess = new ArrayList<UnscoredSearchHit>(accumulatedFilteredDocs.size());
		Iterator<Integer> docIt = accumulatedFilteredDocs.keySet().iterator();
		while(docIt.hasNext()) {
			docsToProcess.add(accumulatedFilteredDocs.get(docIt.next()));
		}
		// sort chronologically
		RealTimeDocChronologicalComparator comparator = new RealTimeDocChronologicalComparator(false);
		Collections.sort(docsToProcess, comparator);

		return docsToProcess;
	}




}
