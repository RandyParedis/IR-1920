package edu.gslis.demo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import org.apache.commons.validator.GenericValidator;

import edu.gslis.docscoring.QueryDocScorer;
import edu.gslis.docscoring.ScorerDirichlet;
import edu.gslis.docscoring.ScorerPLM;
import edu.gslis.docscoring.support.CollectionStats;
import edu.gslis.filtering.session.FilterSession;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.ParameterBroker;
import edu.gslis.utils.Stopper;



/** 
 * Wrapper to run the positional language model (PLM) scorer, re-scoring and
 * re-ranking a set of documents scored by Indri.
 */
public class RunPLM {
	
    public static int DEFAULT_MAX_RESULTS = 1000;
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchFieldException, FileNotFoundException {
		File paramFile = new File(args[0]);
		if(!paramFile.exists()) {
			System.err.println("you must specify a parameter file to run against.");
			System.exit(-1);
		}
		
		ParameterBroker params = new ParameterBroker(args[0]);

		
		GQueries        queries    = new GQueriesJsonImpl();
		queries.setMetadataField(FilterSession.NAME_OF_EMIT_STATUS_FIELD);
		queries.setMetadataField(FilterSession.NAME_OF_CONSTRAINT_FIELD);
		queries.read(params.getParamValue(ParameterBroker.QUERY_PATH_PARAM));
		
		IndexWrapper index = new IndexWrapperIndriImpl(params.getParamValue("index"));
				
		String runId = "gslis";
        if(params.getParamValue("run-name") != null)
            runId = params.getParamValue("run-name");

        String outputFile = params.getParamValue("output");
		
		Stopper stopper = null;
		if(params.getParamValue(ParameterBroker.STOPPER_PARAM) != null)
			stopper = new Stopper(params.getParamValue(ParameterBroker.STOPPER_PARAM));
		
		ClassLoader loader = ClassLoader.getSystemClassLoader();
		
		// figure out how we'll be accessing corpus-level stats
		// default
		String corpusStatsClass = "edu.gslis.docscoring.support.IndexBackedCollectionStats";
		// if we've been told otherwise
		if(params.getParamValue("bg-stat-type") != null)
			corpusStatsClass = params.getParamValue("bg-stat-type");
		CollectionStats corpusStats = (CollectionStats)loader.loadClass(corpusStatsClass).newInstance();
		
		// figure out the source of our corpus-level stats.
		//  this could be the name of a variable pointing to an index, or...
		//  the name of a variable pointing to a dumpindex-type file
		// default
		String corpusStatsPath = params.getParamValue("bg-source-path");
		if(corpusStatsPath != null)
			corpusStats.setStatSource(corpusStatsPath);
		
		// set up our document scorer
		QueryDocScorer docScorer = new ScorerDirichlet();
		docScorer.setCollectionStats(corpusStats);
		
        ScorerPLM  plmScorer = new ScorerPLM();
        plmScorer.setIndex(index);
        String mu = params.getParamValue("plm-mu");
        String sigma = params.getParamValue("plm-sigma");
        plmScorer.setMu(Double.valueOf(mu));
        plmScorer.setSigma(Double.valueOf(sigma));
		    
		Iterator<String> parameterIt = params.getAllParams().keySet().iterator();
		while(parameterIt.hasNext()) {
			String paramName = parameterIt.next();
			if(! paramName.startsWith("scorer-param-"))
				continue;
			String paramValue = params.getParamValue(paramName);
			if (GenericValidator.isDouble(paramValue)) {
			    double doubleValue = Double.parseDouble(params.getParamValue(paramName));
		        paramName = paramName.replaceFirst("scorer-param-", "");
		        docScorer.setParameter(paramName, doubleValue);
			} else {
	            paramName = paramName.replaceFirst("scorer-param-", "");
	            docScorer.setParameter(paramName, paramValue);
			}
		}
		// Perform any optional initialization
		docScorer.init();
		

		Writer outputWriter;
		if (outputFile != null)
		    outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));
		else
		    outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
	    FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance(runId, outputWriter);

	      
		Iterator<GQuery> queryIterator = queries.iterator();
		while(queryIterator.hasNext()) {
			GQuery query = queryIterator.next();
			
			System.err.println(query.getTitle());
			
			FeatureVector surfaceForm = new FeatureVector(stopper);
			Iterator<String> queryTerms = query.getFeatureVector().iterator();
			while(queryTerms.hasNext()) {
				String term = queryTerms.next();
				surfaceForm.addTerm(term, query.getFeatureVector().getFeatureWeight(term));
			}
			query.setFeatureVector(surfaceForm);
			
			docScorer.setQuery(query);

	        SearchHits results = index.runQuery(query, 1000);	            
	        results.rank();

	        // Re-score documents using PLM
	        plmScorer.setQuery(query);
            SearchHits plmResults = new SearchHits();	       
	        for (int i=0; i < results.size(); i++) {
	            SearchHit hit = results.getHit(i);
	            double score = plmScorer.score(hit);	            
	            hit.setScore(score);	            
	            plmResults.add(hit);
	        }
	        plmResults.rank();

            output.write(plmResults, query.getTitle(), 1000);
        }
        output.close();
	}
}
