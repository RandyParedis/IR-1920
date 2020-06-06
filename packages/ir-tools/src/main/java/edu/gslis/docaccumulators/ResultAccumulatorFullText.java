package edu.gslis.docaccumulators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lemurproject.indri.QueryEnvironment;
import lemurproject.indri.ScoredExtentResult;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.searchhits.UnscoredSearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.textrepresentation.IndriDocument;

public class ResultAccumulatorFullText {

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

	public ResultAccumulatorFullText(IndexWrapperIndriImpl indexWrapper, FeatureVector queryModel, String constraint) {
		
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

			Set<Integer> docIDs = new HashSet<Integer>();
			
			if(allResults.length==0)
				return;
			String[] docnos   = env.documentMetadata(allResults, "docno");
			String[] epochs   = env.documentMetadata(allResults, "epoch");


			int k=0;
			for(ScoredExtentResult r: allResults) {
				int docID = r.document;
				if(docIDs.contains(docID)) {
					k++;
					continue;
				}
				docIDs.add(docID);
				String docno = docnos[k];
				double length = (double)env.documentLength(docID);
				IndriDocument doc = new IndriDocument(env);
				doc.setIndex(env);
				double epoch = Double.parseDouble(epochs[k]);
				UnscoredSearchHit hit = new UnscoredSearchHit(docno, docID, length, epoch);
				FeatureVector docVector = doc.getFeatureVector(docID , null);
				Iterator<String> terms = docVector.iterator();
				while(terms.hasNext()) {
					String term = terms.next();
					hit.addFeature(term, docVector.getFeatureWeight(term));
				}
				accumulatedFilteredDocs.put(docID, hit);
				k++;
			}




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
