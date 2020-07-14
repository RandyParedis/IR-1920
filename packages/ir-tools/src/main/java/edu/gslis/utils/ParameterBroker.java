package edu.gslis.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * N.B.  All params are stored as strings.  It is the responsibility of calling classes to transform into
 * appropriate data types. 
 * e.g. mu = Double.parseDouble(paramBroker.getParamValue("mu")
 * 
 * @author Miles Efron
 *
 */
public class ParameterBroker {
	public static final String QUERY_PATH_PARAM = "queries";
	public static final String INDEX_PATH_PARAM = "index";
	public static final String FB_DOCS_PARAM    = "fbDocs";
	public static final String FB_TERMS_PARAM   = "fbTerms";
	public static final String COUNT_PARAM      = "count";
	public static final String STOPPER_PARAM    = "stopper";
	public static final String MU_PARAM         = "mu";
	public static final String QRELS_PATH_PARAM = "qrels";
	
	private static final JsonParser JSON_PARSER = new JsonParser();
	private Map<String,String> params;
	
	
	/**
	 * constructor where we initialize from a json file of structure:
	 * {
	 *  "param1":"value1",
	 *  "param2":"value2"
	 * }
	 * 
	 * @param pathToJson
	 */
	public ParameterBroker(String pathToJson) {
		params = new HashMap<String,String>();
		JsonObject json = null;
		try {
			json = (JsonObject) JSON_PARSER.parse(new BufferedReader(new FileReader(pathToJson)));
		} catch (Exception e) {
			e.printStackTrace();
		}

		Set<Entry<String, JsonElement>> jsonEntries = json.entrySet();
		Iterator<Entry<String, JsonElement>> entryIterator = jsonEntries.iterator();
		while(entryIterator.hasNext()) {
			Entry<String, JsonElement> entry = entryIterator.next();
			params.put(entry.getKey(), entry.getValue().getAsString());
			System.setProperty(entry.getKey(), entry.getValue().getAsString());
		}
	}
	
	
	public String getParamValue(String paramName) {
		if(!params.containsKey(paramName))
			return null;
	    return params.get(paramName);
	}
	
	public void setParam(String name, String value) {
		params.put(name, value);
	}

	// needed if we have a bunch of unusual params to sort through (e.g. multiple indexes, ind1, ind2, ...)
	public Map<String,String> getAllParams() {
		return params;
	}
	
}
