package edu.gslis.docscoring.smart;

import java.util.Iterator;

import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.textrepresentation.FeatureVector;

/** 
 * Below are the various weighting procedures implemented in the SMART 11.0
 * system from libconvert library:

 * weights_idf.c
 * convert.wt_idf.x Reweighting procedure no-op for idf weights.
 * convert.wt_idf.n Reweighting procedure no-op for idf weights.
 * convert.wt_idf.t Reweight vector by multiplying weight by normal idf factor
 * convert.wt_idf.i Reweight vector by multiplying weight by normal idf factor
 * convert.wt_idf.p Reweight vector by multiplying weight by probabilistic idf factor
 * convert.wf_idf.s Reweight vector by multiplying weight by normal idf factor sqared
 * convert.wf_idf.f Reweight vector by dividing weight by collection freq
 * convert.wf_idf.P Reweight phrase vector by multiplying weight by average of term idf factors
 */
public class IDFWeights {
    

    public static final char IDF_NO_OP = 'n';
    public static final char IDF_LOG_WEIGHT = 't';
    public static final char IDF_PROB_WEIGHT = 'p';
    public static final char IDF_FREQ_WEIGHT = 'f';
    public static final char IDF_SQUARE_WEIGHT = 's';
    public static final char IDF_PHRASE_WEIGHT = 'P';

    public static IDFWeight getIDFWeight(char type) throws Exception
    { 
        if (type == IDF_LOG_WEIGHT)
            return new IDFWeights().new LogIDFWeight();
        else if (type == IDF_PROB_WEIGHT)
            return new IDFWeights().new IDFProbWeight();
        else if (type == IDF_FREQ_WEIGHT)
            return new IDFWeights().new IDFFreqWeight();
        else if (type == IDF_SQUARE_WEIGHT)
            return new IDFWeights().new IDFSquareWeight();
        else if (type == IDF_PHRASE_WEIGHT)
            throw new Exception("IDF weight not implemented '" + type + "'");
        else if (type == IDF_NO_OP)
            return new IDFWeights().new IDFWeight();
        else 
            throw new Exception("Unsupported IDF weight type '" + type + "'");
    }    

    // Simple interface for IDFWeights
    public class IDFWeight {
        double numDocs = 0;
        CollectionStats collectionStats = null;

        public void weight(FeatureVector fv) throws Exception {}
        public void setNumDocs(double numDocs) {
            this.numDocs = numDocs;
        }
        
        public void setCollectionStats(CollectionStats collectionStats) {
            this.collectionStats = collectionStats;
        }
    }
    
    // tfidf    new_wt = new_tf * log (num_docs / coll_freq_of_term)
    class LogIDFWeight extends IDFWeight {
        
        public void weight(FeatureVector fv) throws Exception {
            Iterator<String> it = fv.iterator();
            while (it.hasNext()) {
                String term = it.next();
                double weight = fv.getFeatureWeight(term);
                double docFreq = collectionStats.docCount(term);
                if(docFreq > 0)
                    weight = weight * Math.log(numDocs/docFreq);
                fv.setTerm(term, weight);
            } 
        }
    }
    
    // prob    new_wt = new_tf * log ((num_docs - coll_freq)   / coll_freq))
    class IDFProbWeight extends IDFWeight {
        
        public void weight(FeatureVector fv) throws Exception {
            Iterator<String> it = fv.iterator();
            while (it.hasNext()) {
                String term = it.next();
                double weight = fv.getFeatureWeight(term);
                double docFreq = collectionStats.docCount(term);
                weight = weight * Math.log((numDocs - docFreq)/docFreq);
                fv.setTerm(term, weight);
            }         
        }
    }    
    
    //  freq            new_wt = new_tf / n
    class IDFFreqWeight extends IDFWeight {        
        public void weight(FeatureVector fv) throws Exception {
            Iterator<String> it = fv.iterator();
            while (it.hasNext()) {
                String term = it.next();
                double weight = fv.getFeatureWeight(term);
                weight = weight / numDocs;
                fv.setTerm(term, weight);
            }          
        }
    }
    
  //squared     new_wt = new_tf * log(num_docs/coll_freq_of_term)**2
    class IDFSquareWeight extends IDFWeight {
        
        public void weight(FeatureVector fv) throws Exception {
            Iterator<String> it = fv.iterator();
            while (it.hasNext()) {
                String term = it.next();
                double weight = fv.getFeatureWeight(term);
                double docFreq = collectionStats.docCount(term);
                weight = weight * Math.pow(Math.log(numDocs/docFreq), 2);
                fv.setTerm(term, weight);
            }         
        }
    }
    
}
