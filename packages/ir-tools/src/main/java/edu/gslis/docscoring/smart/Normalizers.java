package edu.gslis.docscoring.smart;

import java.util.Iterator;

import edu.gslis.textrepresentation.FeatureVector;

/**
 * weights_norm.c
 * convert.wt_norm.x Reweighting procedure no-op for normalization of weights.
 * convert.wt_norm.n Reweighting procedure no-op for normalization of weights.
 * convert.wt_norm.c Reweight vector by cosine normalization
 * convert.wt_norm.s Reweight vector by normalization with sum of weights
 * convert.wt_norm.C Reweight vector by cosine normalization and store length
 * convert.wt_norm.P Reweight vector by cosine normalization of last stored length
 * u - Pivoted unique normalization (not implemented in SMART 11.0)
 */
public class Normalizers {

    public static final char NORM_NO_OP = 'n';
    public static final char NORM_COSINE = 'c';
    public static final char NORM_PIVOTED_UNIQUE = 'u';
    public static final char NORM_BYTE_SIZE = 'b';
    public static final char NORM_MAX = 'm';
    public static final char NORM_FOURTH = 'f';
    public static final char NORM_SUM = 's';
    
    public static Normalizer getNormalizer(char type) throws Exception
    { 
        if (type == NORM_COSINE)
            return new Normalizers().new CosineNormalizer();
        else if (type == NORM_PIVOTED_UNIQUE)
            return new Normalizers().new PivotedUniqueNormalizer();
        else if (type == NORM_BYTE_SIZE)
            throw new Exception("Normalizer not implemented");
            //return new Normalizers().new ByteSizeNormalizer();
        else if (type == NORM_FOURTH)
            return new Normalizers().new FourthNormalizer();
        else if (type == NORM_MAX)
            return new Normalizers().new MaxNormalizer();
        else if (type == NORM_SUM)
            return new Normalizers().new SumWeightNormalizer();
        else 
            throw new Exception("Unsupported normalizer");
    }   
    
    // Simple interface for Normalizers
    public class Normalizer {
        double avgDocLen = 0;
        double avgUniqueTerms = 0;
        public void normalize(FeatureVector fv) throws Exception {}
        public void setAvgDocLen(double avgDocLen) {
            this.avgDocLen = avgDocLen;
        }
        public void setAvgUniqueTerms(double avgUniqueTerms) {
            this.avgUniqueTerms = avgUniqueTerms;
        }
    }
    
  //sum     divide each new_wt by sum of new_wts in vector
    class SumWeightNormalizer extends Normalizer {
        
        public void weight(FeatureVector fv) {
            Iterator<String> it = fv.iterator();
            double sum = 0;
            it = fv.iterator();
            while (it.hasNext()) {
                String term = it.next();
                sum+= fv.getFeatureWeight(term);
            }         
                    
            it = fv.iterator();
            while (it.hasNext()) {
                String term = it.next();
                double weight = fv.getFeatureWeight(term);
                weight = weight / sum;
                fv.setTerm(term, weight);
            }         
        }
    }    
    
    /**
        divide each new_wt by sqrt (sum of (new_wts squared) )
        This is the usual cosine normalization (I.e. an
        inner product function of two cosine normalized
        vectors will yield the same results as a cosine
        function on vectors (either normalized or not))
    */
    class CosineNormalizer extends Normalizer {
    
        //cosine  divide each new_wt by sqrt (sum of(new_wts squared)) 
        //       This is the usual cosine normalization 
        //        (I.e. an  inner product function of two cosine normalized 
        //        vectors will yield the same results as a cosine function on 
        //        vectors (either normalized or not))
        public void normalize(FeatureVector fv) {
            Iterator<String> it = fv.iterator();
            double sum_squares = 0;
            it = fv.iterator();
            while (it.hasNext()) {
                String term = it.next();
                sum_squares+= Math.pow(fv.getFeatureWeight(term), 2);
            }         
                    
            sum_squares = Math.sqrt(sum_squares);
            it = fv.iterator();
            while (it.hasNext()) {
                String term = it.next();
                double weight = fv.getFeatureWeight(term);
                weight = weight / sum_squares;
                fv.setTerm(term, weight);
            }         
        }
    }
    
  //fourth  divide each new_wt by sum of (new_wts ** 4)
    class FourthNormalizer extends Normalizer {
        
        public void normalize(FeatureVector fv) {
            Iterator<String> it = fv.iterator();
            double sum = 0;
            it = fv.iterator();
            while (it.hasNext()) {
                String term = it.next();
                sum+= Math.pow(fv.getFeatureWeight(term), 4);
            }         
                    
            it = fv.iterator();
            while (it.hasNext()) {
                String term = it.next();
                double weight = fv.getFeatureWeight(term);
                weight = weight / sum;
                fv.setTerm(term, weight);
            }         
        }
    }
    
  //max     divide each new_wt by max new_wt in vector
    class MaxNormalizer extends Normalizer {
        
        public void normalize(FeatureVector fv) {
            Iterator<String> it = fv.iterator();
            double max = 0;
            it = fv.iterator();
            while (it.hasNext()) {
                String term = it.next();
                double weight = fv.getFeatureWeight(term);
                if (weight > max) {
                    max = weight;
                }
            }         
    
            max += 0.00001;
            
            it = fv.iterator();
            while (it.hasNext()) {
                String term = it.next();
                double weight = fv.getFeatureWeight(term);
                weight = weight / max;
                fv.setTerm(term, weight);
            }         
        }   
    }    
    
    /**
     * Pivoted unique normalization as described by Singhal (1996). 
     * This uses the average number of unique terms per document as
     * the pivot. At this time, the value must be provided through 
     * configuration (e.g., output of dumpindex for index).
     */
    class PivotedUniqueNormalizer extends Normalizer {
        public void normalize(FeatureVector fv, int docLen) throws Exception 
        {
            // The pivot is the average number of unique terms per document.
            double slope = 2.0;
            Iterator<String> it = fv.iterator();
            it = fv.iterator();
            while (it.hasNext()) {
                String term = it.next();
                double weight = fv.getFeatureWeight(term);
                weight = (1.0 - slope)*avgUniqueTerms + (slope*(docLen/fv.getLength()));
                fv.setTerm(term,  weight);
            }
        }
    }
}

