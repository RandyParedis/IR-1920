package com.stackoverflow.helper;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import java.util.*;

public class Vector {
    private Map<String, Number> contents = new HashMap<>();

    static double epsilon = 0.000000001;

    public Vector() {}

    public Vector(Map<String, Number> vector) {
        contents = vector;
    }

    public boolean hasKey(String key) {
        return contents.containsKey(key);
    }

    public Number obtain(String name) {
        return contents.get(name);
    }

    public Set<Map.Entry<String, Number>> entrySet() {
        return contents.entrySet();
    }

    public int size() {
        return contents.size();
    }

    public void insert(String name, Number value) {
        contents.put(name, value);
    }

    public void insertAll(Vector vector) {
        contents.putAll(vector.contents);
    }

    public void insertAll(Map<String, Number> vector) {
        contents.putAll(vector);
    }

    public void delete(String name) {
        contents.remove(name);
    }

    public Vector copy() {
        return new Vector(this.contents);
    }

    public Vector add(Vector other) {
        for(Map.Entry<String, Number> entry: other.contents.entrySet()) {
            addFor(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public Vector add(Number other) {
        contents.replaceAll((k, v) -> v.doubleValue() + other.doubleValue());
        return this;
    }

    public void addFor(String term, Number value) {
        contents.computeIfPresent(term, (k, v) -> v.doubleValue() + value.doubleValue());
        contents.putIfAbsent(term, value);
    }

    public Vector subtract(Vector other) {
        for(Map.Entry<String, Number> entry: other.contents.entrySet()) {
            subtractFor(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public Vector subtract(Number other) {
        contents.replaceAll((k, v) -> v.doubleValue() - other.doubleValue());
        return this;
    }

    public void subtractFor(String term, Number value) {
        addFor(term, - value.doubleValue());
    }

    public Vector multiply(Number other) {
        contents.replaceAll((k, v) -> v.doubleValue() * other.doubleValue());
        return this;
    }

    public Vector divide(Number other) {
        contents.replaceAll((k, v) -> v.doubleValue() / other.doubleValue());
        return this;
    }

    public Vector inverse() {
        contents.replaceAll((k, v) -> 1.0 / v.doubleValue());
        return this;
    }

    public Vector mean() {
        Vector vec = copy();
        return vec.divide(vec.size());
    }

    public Number sum() {
        return contents.values().stream().reduce(0, (s, num) -> s.doubleValue() + num.doubleValue());
    }

    public Number min() {
        Number min = Double.POSITIVE_INFINITY;
        for(Map.Entry<String, Number> entry: entrySet()) {
            Number val = entry.getValue();
            if(entry.getValue().doubleValue() < min.doubleValue()) {
                min = val;
            }
        }
        return min;
    }

    public Number max() {
        Number max = Double.NEGATIVE_INFINITY;
        for(Map.Entry<String, Number> entry: entrySet()) {
            Number val = entry.getValue();
            if(entry.getValue().doubleValue() > max.doubleValue()) {
                max = val;
            }
        }
        return max;
    }

    public Vector normalize() {
        return divide(sum());
    }

    public Vector normalized() {
        Vector vec = copy();
        return vec.divide(vec.sum());
    }

    public boolean isNormalized() {
        return isNormalized(epsilon);
    }

    public boolean isNormalized(double epsilon) {
        Number s = sum();
        return Math.abs(s.doubleValue() - 1) < epsilon;
    }

    public Query toQuery(Analyzer analyzer) throws ParseException {
        return toQuery(analyzer, epsilon);
    }

    public Query toQuery(Analyzer analyzer, double epsilon) throws ParseException {
        StringBuilder qstring = new StringBuilder();
        for(Map.Entry<String, Number> entry: entrySet()) {
            double value = entry.getValue().doubleValue();
            if(Math.abs(value) > epsilon) {
                if(value > 0.0) {
                    qstring.append(entry.getKey()).append("^").append(value).append(" ");
                } else {
                    qstring.append("-").append(entry.getKey()).append("^").append(Math.abs(value)).append(" ");
                }
            }
        }
        MultiFieldQueryParser mfqp = new MultiFieldQueryParser(new String[]{"body", "answers"}, analyzer);
        mfqp.setDefaultOperator(QueryParser.Operator.OR);
        return mfqp.parse(qstring.toString());
    }
}
