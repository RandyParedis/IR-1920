package edu.gslis.queries;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.KeyValuePair;
import edu.gslis.utils.ScorableComparator;

/**
 * A container for holding a bunch of GQuery objects, with various types of convenience functionality added in 
 * instantiating classes.
 * 
 * @author Miles Efron
 *
 */
public abstract class GQueries implements Iterable<GQuery> {
   protected List<GQuery> queryList;
   protected Map<String,Integer> nameToIndex;
   protected Set<String> metadataFields;
   protected Map<String, String> metadata;

   public abstract void read(String pathToQueries);
		   
	
   public GQueries() {
       metadataFields = new HashSet<String>();
       metadata = new TreeMap<String, String>();
   }

   public void addQuery(GQuery query) {
        if(queryList == null)
            queryList = new LinkedList<GQuery>();
        
        queryList.add(query);
        
        if(nameToIndex==null)
            nameToIndex = new HashMap<String,Integer>();
        
        nameToIndex.put(query.getTitle(), queryList.size()-1);
        
    }
    
    public GQuery getIthQuery(int i) {
        if(queryList == null || i >= queryList.size()) {
            System.err.println("died trying to get query number " + i + "  when we have only " + queryList.size() + " queries.");
            System.exit(-1);        
        }
        return queryList.get(i);
    }
    
    public GQuery getNamedQuery(String queryName) {
        if(queryList == null || ! nameToIndex.containsKey(queryName)) {
            System.err.println("died trying to get query  " + queryName + ".");
            System.exit(-1);        }
        return queryList.get(nameToIndex.get(queryName));
    }
    

    public Iterator<GQuery> iterator() {
        return queryList.iterator();
    }

    public int numQueries() {
        return queryList.size();
    }
    

    @Override
    public String toString() {
        DecimalFormat format = new DecimalFormat("#.##########################");
        JsonObject outputObjects = new JsonObject();
        JsonArray outputJsonArray = new JsonArray();

        JsonObject outputMetadata = new JsonObject();
        for (String key: metadata.keySet()) {
            outputMetadata.addProperty(key, metadata.get(key));
        }
        outputObjects.add("metadata", outputMetadata);

        
        Iterator<GQuery> queryIterator = queryList.iterator();
        while(queryIterator.hasNext()) {
            GQuery query = queryIterator.next();

            JsonObject outputQueryObject = new JsonObject();
            outputQueryObject.addProperty("title", query.getTitle());
            outputQueryObject.addProperty("text", query.getText());
            
            if(query.getMetadata("epoch") != null)
                outputQueryObject.addProperty("epoch", query.getMetadata("epoch"));
            if(query.getMetadata("querytweettime") != null)
                outputQueryObject.addProperty("querytweettime", query.getMetadata("querytweettime"));           
            if(query.getMetadata("group") != null)
                outputQueryObject.addProperty("group", query.getMetadata("group"));
            if(query.getMetadata("timestamp") != null)
                outputQueryObject.addProperty("timestamp", query.getMetadata("timestamp"));
            
            Iterator<String> fields = metadataFields.iterator();
            while(fields.hasNext()) {
                String field = fields.next();
                if(query.getMetadata(field) != null)
                    outputQueryObject.addProperty(field, query.getMetadata(field));
            }
            
            JsonArray modelArray = new JsonArray();
            FeatureVector featureVector = query.getFeatureVector();
            List<KeyValuePair> pairs = new ArrayList<KeyValuePair>(featureVector.getFeatureCount());
            Iterator<String> featureIterator = featureVector.iterator();
            while(featureIterator.hasNext()) {
                String feature = featureIterator.next();
                double weight  = featureVector.getFeatureWeight(feature);
                KeyValuePair tuple = new KeyValuePair(feature,weight);
                pairs.add(tuple);
            }
            ScorableComparator comparator = new ScorableComparator(true);
            Collections.sort(pairs, comparator);
            
            Iterator<KeyValuePair> tupleIterator = pairs.iterator();
            while(tupleIterator.hasNext()) {
                KeyValuePair tuple = tupleIterator.next();
                String feature = tuple.getKey();
                double weight  = tuple.getScore();
                
                JsonObject tupleObject = new JsonObject();
                tupleObject.addProperty("weight", format.format(weight));
                tupleObject.addProperty("feature", feature);
                modelArray.add(tupleObject);
            }
            outputQueryObject.add("model", modelArray);


            outputJsonArray.add(outputQueryObject);
        }   
        outputObjects.add("queries", outputJsonArray);
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(outputObjects);
        return json;
    }

    public void setMetadataField(String fieldName) {
        metadataFields.add(fieldName);
    }

    public void setMetadata(String key, String value) {
        metadata.put(key, value);
    }
	
}
