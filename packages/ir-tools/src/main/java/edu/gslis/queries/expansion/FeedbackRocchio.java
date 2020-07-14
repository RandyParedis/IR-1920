package edu.gslis.queries.expansion;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.KeyValuePair;
import edu.gslis.utils.KeyValuePairs;




public class FeedbackRocchio extends Feedback {
	private boolean adHocGroom = false;
	
	@Override
	public void build() {
		try {
			Set<String> vocab = new HashSet<String>();
			List<FeatureVector> fbDocVectors = new LinkedList<FeatureVector>();

			if(relDocs == null) {
				relDocs = index.runQuery(originalQuery, fbDocCount);
			}


			
			Iterator<SearchHit> hitIterator = relDocs.iterator();
			while(hitIterator.hasNext()) {
				SearchHit hit = hitIterator.next();
				FeatureVector docVector = index.getDocVector(hit.getDocID(), stopper);
				docVector.toIdf(index, false);
				docVector.l2Normalize();
				vocab.addAll(docVector.getFeatures());
				fbDocVectors.add(docVector);
			}

			features = new KeyValuePairs();

			
			Iterator<String> it = vocab.iterator();
			while(it.hasNext()) {
				String term = it.next();


				
				if(adHocGroom && (term.matches(".*[0-9].*") || term.length()<3)) {
					continue;
				}
				
				double fbWeight = 0.0;
				
				

				Iterator<FeatureVector> docIT = fbDocVectors.iterator();
				while(docIT.hasNext()) {
					FeatureVector docVector = docIT.next();
					double docProb = docVector.getFeatureWeight(term);
					fbWeight += docProb;
				}
				fbWeight /= (double)fbDocVectors.size();
				
				KeyValuePair tuple = new KeyValuePair(term, fbWeight);
				features.add(tuple);
			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setGroom(boolean groomModels) {
		this.adHocGroom = groomModels;
	}




}
