package edu.gslis.lucene.main.config;
import java.util.Set;


public class IndexConfig {
    String indexPath = "";
    CorpusConfig corpus;
    
    String stopwords = "";
    String analyzer = "";
    String similarity = "";
    Set<FieldConfig> fields;
    public String getIndexPath() {
        return indexPath;
    }
    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }
    public CorpusConfig getCorpus() {
        return corpus;
    }
    public void setCorpus(CorpusConfig corpus) {
        this.corpus = corpus;
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
    public Set<FieldConfig> getFields() {
        return fields;
    }
    public void setFields(Set<FieldConfig> fields) {
        this.fields = fields;
    }
}