package edu.gslis.lucene.main.config;

import java.util.Set;


public class RunQueryConfig {
    String index = "";
    Set<QueryConfig> queries;
    String stopwords = "";
    String analyzer = "";
    String similarity = "";
    String field = "";
    String docno = "";
    QueryFile queryFile;

    
    public String getIndex() {
        return index;
    }
    public void setIndex(String index) {
        this.index = index;
    }
    public Set<QueryConfig> getQueries() {
        return queries;
    }
    public void setQueries(Set<QueryConfig> queries) {
        this.queries = queries;
    }
    public String getStopwords() {
        return stopwords;
    }
    public void setStopwords(String stopwords) {
        this.stopwords = stopwords;
    }
    public String getAnalyzer() {
        return analyzer;
    }
    public void setAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }
    public String getSimilarity() {
        return similarity;
    }
    public void setSimilarity(String similarity) {
        this.similarity = similarity;
    }
    public String getField() {
        return field;
    }
    public void setField(String field) {
        this.field = field;
    }
    public String getDocno() {
        return docno;
    }
    public void setDocno(String docno) {
        this.docno = docno;
    }
    public QueryFile getQueryFile() {
        return queryFile;
    }
    public void setQueryFile(QueryFile queryFile) {
        this.queryFile = queryFile;
    }
}