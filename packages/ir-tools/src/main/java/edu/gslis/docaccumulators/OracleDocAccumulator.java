package edu.gslis.docaccumulators;

import java.util.Iterator;
import java.util.Set;

import edu.gslis.eval.Qrels;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;

public class OracleDocAccumulator {

	public static SearchHits getAllRelDocs(GQuery query, IndexWrapper index, Qrels qrels) {
		SearchHits relDocs = new SearchHits();
		Set<String> docnos = qrels.getRelDocs(query.getTitle());
		if(docnos == null || docnos.size() == 0) 
			return relDocs;
		Iterator<String> docIt = docnos.iterator();
		while(docIt.hasNext()) {
			String docno = docIt.next();
			int docId = index.getDocId(docno);
			if(docId < 1)
				continue;
			double score = Double.NEGATIVE_INFINITY;
			SearchHit doc = new SearchHit();
			doc.setDocID(docId);
			doc.setDocno(docno);
			doc.setScore(score);
			relDocs.add(doc);
		}
		return relDocs;
	}
}
