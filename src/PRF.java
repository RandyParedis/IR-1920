import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PRF {
    private IndexReader reader;
    private Map<Integer, Vector> cache = new HashMap<>();

    public PRF(IndexReader reader) {
        this.reader = reader;
    }

    public Vector getQueryVector(String query) {
        String altered = Helper.transform(query);
        String[] split = altered.split("\\s+");
        Vector res = new Vector();
        for(String s: split) {
            res.addFor(s, 1);
        }
        return res;
    }

    public Vector getFeatureVector(int docId) throws IOException {
        if(cache.containsKey(docId)) {
            return cache.get(docId);
        }
        Fields flds = reader.getTermVectors(docId);
        Iterator<String> fldIt = flds.iterator();
        String next = null;
        BytesRef text = null;
        Vector frequencies = new Vector();
        while(fldIt.hasNext()) {
            next = fldIt.next();
            Terms vector = flds.terms(next);
            TermsEnum termsEnum = vector.iterator();
            while ((text = termsEnum.next()) != null) {
                String term = text.utf8ToString();
                int freq = (int) termsEnum.totalTermFreq();
                frequencies.addFor(term, freq);
            }
        }
        cache.put(docId, frequencies);
        return frequencies;
    }

    public Vector sum(List<Integer> docIds) throws IOException {
        Vector sum = new Vector();
        for(Integer id: docIds) {
            sum.add(getFeatureVector(id));
        }
        return sum;
    }

    public Vector rocchio(Vector query, Vector relevant, Vector irrelevant, Number alpha, Number beta, Number gamma) {
        return query.copy().multiply(alpha).add(relevant.mean().multiply(beta)).subtract(irrelevant.mean().multiply(gamma));
    }
}
