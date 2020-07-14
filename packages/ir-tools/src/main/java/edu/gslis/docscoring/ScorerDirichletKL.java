package edu.gslis.docscoring;

import java.util.Iterator;

import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;


/**
 * Preliminary KL-divergence scorer implementation with Dirichlet smoothing. 
 */
public class ScorerDirichletKL extends QueryDocScorer 
{
	public static final String MU = "mu";
	
	public ScorerDirichletKL() {
		setParameter(MU, 2500);
	}
	public void setQuery(GQuery query) {
		this.gQuery = query;
	}

   
    public double score(SearchHit doc) 
    {
        double logLikelihood = 0.0;
        Iterator<String> queryIterator = gQuery.getFeatureVector().iterator();
        
        double mu = paramTable.get(MU);

        double dl = doc.getLength();
                        
        while(queryIterator.hasNext()) {            
            String feature = queryIterator.next();
            double cwd = doc.getFeatureVector().getFeatureWeight(feature);
            
            // p(w|C)
            double pwc =  collectionStats.termCount(feature) / collectionStats.getTokCount();
            // p(w|Q)
            double pwq =  gQuery.getFeatureVector().getFeatureWeight(feature)/
                               gQuery.getFeatureVector().getFeatureCount();            
            // p(w|D)
            double pwd = (cwd + mu * pwc)/(dl + mu);
           
            logLikelihood += pwq*Math.log(pwq) - pwq*Math.log(pwd);
        }
        
        return -1*logLikelihood;
    }   
   
}
