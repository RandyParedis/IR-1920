package edu.gslis.lucene.main.config;


public class QueryConfig implements Comparable<QueryConfig> {
    String number; 
    String text;
    public String getNumber() {
        return number;
    }
    public void setNumber(String number) {
        this.number = number;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    
    public int compareTo(QueryConfig qc) {
        return number.compareTo(qc.getNumber());
    }
    
}