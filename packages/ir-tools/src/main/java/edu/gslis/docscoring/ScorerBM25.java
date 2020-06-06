package edu.gslis.docscoring;

import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;

// Based on BM25 as described in Croft, Metzler & Strohman textbook, pp. 250-252
// Default parameters from Manning, Raghavan & Schutze,
//	http://nlp.stanford.edu/IR-book/html/htmledition/okapi-bm25-a-non-binary-model-1.html
public class ScorerBM25 extends QueryDocScorer {
	public static final String PARAM_K1_NAME = "k1";
	public static final String PARAM_K2_NAME = "k2";
	public static final String PARAM_B_NAME = "b";

	private static final double K1_DEFAULT = 1.2;
	private static final double B_DEFAULT = 0.75;
	private static final double K2_DEFAULT = 2.0;

	public ScorerBM25(GQuery query, CollectionStats stats, double k1, double b, double k2) {
		gQuery = query;
		collectionStats = stats;
		paramTable.put(PARAM_K1_NAME, k1);
		paramTable.put(PARAM_B_NAME, b);
		paramTable.put(PARAM_K2_NAME, k2);
	}

	public ScorerBM25() {

	}

	// Manning states that the query weight term is unnecessary for short queries.
	// If you instantiate specifying k1 and b but not k2, we assume you want to disable this term.
	public ScorerBM25(GQuery query, CollectionStats stats, double k1, double b) {
		this(query, stats, k1, b, -1.0);
	}

	// If not specified, set default parameters to those specified in Manning book.
	public ScorerBM25(GQuery query, CollectionStats stats) {
		this(query, stats, K1_DEFAULT, B_DEFAULT, K2_DEFAULT);
	}

	public double score(SearchHit doc) {
		double N = collectionStats.getDocCount();
		double totalTerms = collectionStats.getTokCount();
		double avgDocLength = totalTerms / N;

		double docLength = doc.getLength();

		double docScore = 0;

		Iterator<String> queryIt = this.gQuery.getFeatureVector().iterator();
		while (queryIt.hasNext()) {
			String term = queryIt.next();

			double termFreq = doc.getFeatureVector().getFeatureWeight(term);

			double n = collectionStats.docCount(term);
			double idf = Math.log((N - n + 0.5) / (n + 0.5));

			double K = paramTable.get(PARAM_K1_NAME) * ((1 - paramTable.get(PARAM_B_NAME)) + paramTable.get(PARAM_B_NAME) * (docLength / avgDocLength));
			double documentScaling = ((termFreq * (paramTable.get(PARAM_K1_NAME) + 1)) / (K + termFreq));

			double termScore = idf * documentScaling;

			// If we're including the query weight term
			// (i.e. if k2 is non-negative)
			if (paramTable.get(PARAM_K2_NAME) >= 0) {
				double termFreqQuery = this.gQuery.getFeatureVector().getFeatureWeight(term);
				double queryScaling = (paramTable.get(PARAM_K2_NAME) + 1) * termFreqQuery / (paramTable.get(PARAM_K2_NAME) + termFreqQuery);
				termScore *= queryScaling;
			}

			// add to doc score
			docScore += termScore;
		}

		return docScore;
	}

}
