package edu.gslis.queries;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.gslis.textrepresentation.FeatureVector;



public class GQueriesIndriImpl extends GQueries {

	public void read(String pathToQueries) {
	    
	    try
	    {
    	    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse (new File(pathToQueries));
            NodeList queries = doc.getElementsByTagName("query");
            for (int i=0; i<queries.getLength(); i++) {
                Node node = queries.item(i);
                if(node.getNodeType() == Node.ELEMENT_NODE) {
                    Element query = (Element)node;
                    String number = query.getElementsByTagName("number").item(0).getFirstChild().getNodeValue();;
                    String text = query.getElementsByTagName("text").item(0).getFirstChild().getNodeValue();
                    text = text.replaceAll("\\\n", "");
                    FeatureVector fv = new FeatureVector(text, null);

                    GQuery gquery = new GQuery();
                    gquery.setText(text);
                    gquery.setTitle(number);
                    gquery.setFeatureVector(fv);
                
                    addQuery(gquery);                    
                }
            }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
}
