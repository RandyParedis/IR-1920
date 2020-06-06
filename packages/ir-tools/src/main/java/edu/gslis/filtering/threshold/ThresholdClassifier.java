package edu.gslis.filtering.threshold;

public abstract class ThresholdClassifier {
	protected double threshold = Double.NEGATIVE_INFINITY;
	
	public double getThreshold() {
		return threshold;
	}
	public boolean emit(double score) {
		return (score > threshold) ? true : false;
	}
}
