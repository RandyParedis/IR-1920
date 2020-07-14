package edu.gslis.docscoring;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.NormalDistribution;

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.textrepresentation.FeatureVector;

/**
 * Implements the positional language model 
 * 
 * Position language model
 * @author cwillis
 */
public class ScorerPLM extends QueryDocScorer {

       
    double sigma = 50;
    double mu = 2500;
    
    IndexWrapper index;
    
    public void setIndex(IndexWrapper index) {
        this.index = index;
    }
    
    public void setMu(double mu) {
        this.mu = mu;
    }
    
    public void setSigma(double sigma) {
        this.sigma = sigma;
    }
    
    public double getPassageLength(int pos, double docLen, double sigma)
    {
        NormalDistribution dnorm = new NormalDistribution(pos, sigma);

        double psgLen = Math.sqrt(2 * Math.PI) * sigma * 
                (dnorm.cumulativeProbability(docLen) - dnorm.cumulativeProbability(0));
        
        return psgLen;
    }
    
    public double propagationCount(double distance) {
        return Math.exp( - distance * distance / 2);
    }
    
    private double kl(GQuery query, FeatureVector dlm, double docLen) 
    {
        
        Iterator<String> queryIterator = gQuery.getFeatureVector().iterator();
        double ll = 0;
        while(queryIterator.hasNext()) {
            String feature = queryIterator.next();
            double docFreq = dlm.getFeatureWeight(feature);
            double cpr = index.termFreq(feature) / index.termCount();            
            double pr = (docFreq + mu * cpr) / (docLen + mu);
            double qw = gQuery.getFeatureVector().getFeatureWeight(feature);
            if (pr > 0)
                ll += qw * Math.log(pr);
        }
        return ll;
    }
    
    public String getPassage(int center, List<String> docTerms, double len) {
        StringBuffer passage = new StringBuffer();
        int start = 0, end = docTerms.size();
        if (center > (len/2)) 
            start = center - (int)(len/2);
        if (docTerms.size() > (center + len/2)) 
            end = center + (int) len/2;
        
        for (int i=start; i < end; i++) {
            passage.append(docTerms.get(i));
            passage.append(" ");
        }
        return passage.toString();

    }



    public double score(SearchHit doc) 
    {      
        double score = -10000;
        
        // Ordered list of terms in this document
        List<String> docTerms = index.getDocTerms(doc.getDocID());

        // Map to store the query terms and positions
        Map<Integer, String> qposMap = new TreeMap<Integer, String>();

        // Set of query terms
        Set<String> qterms = gQuery.getFeatureVector().getFeatures();

        // First pass: get position of query terms
        for (int i=0; i< docTerms.size(); i++)
        {
            String term = docTerms.get(i);
            if (qterms.contains(term)) {
                qposMap.put(i, term);
            }
        }
        
        FeatureVector plm = new FeatureVector(null);
        int docLen = docTerms.size();
        
        // Only score positions where query terms occur
        for (int i: qposMap.keySet()) {
            double psgLen = getPassageLength(i, docLen, sigma);
            
            for (int j: qposMap.keySet()) {
                if (j == i) continue;
                
                String term = qposMap.get(j);
                
                double distance = Math.abs(j - i) / sigma;
                double pr =  Math.exp( - distance * distance / 2) / psgLen;
                if (pr > 0) {
                    plm.addTerm(term, pr);
                }
            }            
            // KL score for the passage centerd on term i
            double plmScore = kl(gQuery, plm, psgLen);
            
            //String passage = getPassage(i, docTerms, psgLen);
            
            if (plmScore > score) {
                //System.out.println(doc.getDocno() + "," + plmScore + ", " + passage);
                score = plmScore;
            }
        }
        return score;       
    }
}
