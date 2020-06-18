package com.stackoverflow.searching;

public class InfoCustom {
    boolean wordDelimiter;
    boolean trim;
    boolean porter;
    int ngram;
    boolean synonym;

    public InfoCustom(boolean w, boolean t, boolean p, int n, boolean s) {
        wordDelimiter = w;
        trim = t;
        porter = p;
        ngram = n;
        synonym = s;
    }

}
