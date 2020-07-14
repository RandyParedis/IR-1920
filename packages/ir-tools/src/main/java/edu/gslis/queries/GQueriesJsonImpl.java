package edu.gslis.queries;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.gslis.textrepresentation.FeatureVector;


/**
 * reads and holds GQueries stored as a serialized JSON file on disk.
 * 
 * @author Miles Efron
 *
 */
public class GQueriesJsonImpl extends GQueries {

	private static final JsonParser JSON_PARSER = new JsonParser();
	
	
	public void read(String pathToQueries) {
		JsonObject obj = null;
		try {
			obj = (JsonObject) JSON_PARSER.parse(new BufferedReader(new FileReader(pathToQueries)));
		} catch (Exception e) {
			System.err.println("died reading queries from json file");
			e.printStackTrace();
			System.exit(-1);
		}

		
		JsonArray queryObjectArray = obj.getAsJsonArray("queries");
		queryList = new ArrayList<GQuery>(queryObjectArray.size());
		nameToIndex = new HashMap<String,Integer>(queryList.size());
		Iterator<JsonElement> queryObjectIterator = queryObjectArray.iterator();
		int k=0;
		while(queryObjectIterator.hasNext()) {
			JsonObject queryObject = (JsonObject) queryObjectIterator.next();
			String title = queryObject.get("title").getAsString();
			String text  = queryObject.get("text").getAsString();
			
			nameToIndex.put(title, k++);
			FeatureVector featureVector = new FeatureVector(null);
			JsonArray modelObjectArray = queryObject.getAsJsonArray("model");
			Iterator<JsonElement> featureIterator = modelObjectArray.iterator();
			while(featureIterator.hasNext()) {
			    Object elem = featureIterator.next();
			    if (elem instanceof JsonNull)
			        continue;
				JsonObject featureObject = (JsonObject)elem;
				double weight  = featureObject.get("weight").getAsDouble();
				String feature = featureObject.get("feature").getAsString();
				featureVector.addTerm(feature, weight);
			}
			
			
			GQuery gQuery = new GQuery();
			gQuery.setTitle(title);
			gQuery.setText(text);
			gQuery.setFeatureVector(featureVector);
			
			if(queryObject.get("epoch") != null) {
				gQuery.setMetadata("epoch", queryObject.get("epoch").getAsString());
			}
			if(queryObject.get("querytweettime") != null) {
				gQuery.setMetadata("querytweettime", queryObject.get("querytweettime").getAsString());
			}
			if(queryObject.get("group") != null) {
				gQuery.setMetadata("group", queryObject.get("group").getAsString());
			}
			
			Iterator<String> fields = metadataFields.iterator();
			while(fields.hasNext()) {
				String field = fields.next();
				if(queryObject.get(field) != null) {
					gQuery.setMetadata(field, queryObject.get(field).getAsString());
				}
			}
			
			queryList.add(gQuery);
			
		}	
	}
}
