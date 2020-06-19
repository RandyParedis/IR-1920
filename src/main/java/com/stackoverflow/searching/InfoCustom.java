package com.stackoverflow.searching;

public class InfoCustom {
    public boolean wordDelimiter;
    public boolean trim;
    public boolean porter;
    public int ngram;
    public boolean synonym;

    public InfoCustom(boolean w, boolean t, boolean p, int n, boolean s) {
        wordDelimiter = w;
        trim = t;
        porter = p;
        ngram = n;
        synonym = s;
    }

}
