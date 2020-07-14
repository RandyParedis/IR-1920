package edu.gslis.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListUtils {

	public static double sum(List<Double> x) {
		double ss = 0.0;
		Iterator<Double> it = x.iterator();
		while(it.hasNext()) {
			ss += it.next();
		}
		return ss;
	}
	
	public static double[] toArray(List<Double> x) {
		double[] y = new double[x.size()];
		Iterator<Double> xIt = x.iterator();
		int k=0;
		while(xIt.hasNext()) {
			y[k++] = xIt.next();
		}
		return y;
	}
	
	public static List<Double> scale(List<Double> data, double factor) {
		List<Double> y = new ArrayList<Double>(data.size());
		Iterator<Double> it = data.iterator();
		while(it.hasNext()) {
			y.add(it.next() * factor);
		}
		return y;
	}
	
	// gives x-landmark if isMax==true, else landmark-x.
	public static List<Double> tare(List<Double> data, double landmark, boolean isMax) {
		List<Double> y = new ArrayList<Double>(data.size());
		Iterator<Double> it = data.iterator();
		while(it.hasNext()) {
			if(isMax) {
				y.add(it.next() - landmark);
			} else {
				y.add(landmark - it.next());
			}
		}
		return y;
	}
}
