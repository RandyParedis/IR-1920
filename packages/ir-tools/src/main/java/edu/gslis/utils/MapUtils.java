package edu.gslis.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MapUtils {

	public static Map<String,Double> sumToOne(Map<String,Double> in) {
		double sum = MapUtils.sumValues(in);
		Map<String, Double> out = new HashMap<String,Double>(in.size());
		Iterator<String> inIt = in.keySet().iterator();
		while(inIt.hasNext()) {
			String key = inIt.next();
			Double val = (Double)in.get(key) / sum;
			out.put(key, val);
		}
		return out;
	}
	
	public static double sumValues(Map<String,Double> in) {
		double sum = 0.0;
		Iterator<String> inIt = in.keySet().iterator();
		while(inIt.hasNext()) {
			sum += (Double)in.get(inIt.next());
		}
		return sum;
	}
}
