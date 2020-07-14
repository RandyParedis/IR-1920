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
import edu.gslis.docaccumulators.Postings;
import edu.gslis.docaccumulators.PostingsAggregator;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.searchhits.UnscoredSearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.textrepresentation.IndriDocument;

public class ResultAccumulatorFullTextNew {

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

	public ResultAccumulatorFullTextNew(IndexWrapperIndriImpl indexWrapper, FeatureVector queryModel, String constraint) {
		
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

			Map<Integer,UnscoredSearchHit> fullText = new HashMap<Integer,UnscoredSearchHit>(accumulatedFilteredDocs.size());
			Iterator<Integer> it = accumulatedFilteredDocs.keySet().iterator();
			while(it.hasNext()) {
				int docID = it.next();
				UnscoredSearchHit hit = accumulatedFilteredDocs.get(docID);
				IndriDocument doc = new IndriDocument(env);
				doc.setIndex(env);
				FeatureVector docVector = doc.getFeatureVector(docID , null);
				Iterator<String> terms = docVector.iterator();
				while(terms.hasNext()) {
					String term = terms.next();
					hit.addFeature(term, docVector.getFeatureWeight(term));
				}
				fullText.put(docID, hit);
			}
			accumulatedFilteredDocs = fullText;
			
			



		} catch (Exception e) {
			e.printStackTrace();
		}
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
