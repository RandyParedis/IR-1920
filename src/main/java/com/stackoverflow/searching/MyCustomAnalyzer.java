package com.stackoverflow.searching;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.pattern.PatternReplaceFilter;
import org.apache.lucene.analysis.miscellaneous.TrimFilter;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.StopwordAnalyzerBase;

import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.CATENATE_ALL;

import java.io.*;

import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.util.CharsRef;

// https://www.baeldung.com/lucene-analyzers
public class MyCustomAnalyzer extends StopwordAnalyzerBase {
    private SynonymMap generateSynonymMap() {
        BufferedReader reader;
        try {
            SynonymMap.Builder builder = new SynonymMap.Builder(true);
            reader = new BufferedReader(new FileReader("data/en_thesaurus_synonyms.txt"));
            String line = reader.readLine();
            JSONParser parser = new JSONParser();
            while (line != null) {
                JSONObject json = (JSONObject) parser.parse(line);
                String word = (String) json.get("word");
                JSONArray jsonArray = (JSONArray) json.get("synonyms");
                jsonArray.forEach(item -> {
//                    System.out.println(word + ": " + item);
                    builder.add(new CharsRef(word), new CharsRef((String) item), true);
                });
                line = reader.readLine();
            }
            reader.close();
            SynonymMap synMap = builder.build();
            return synMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        StandardTokenizer src = new StandardTokenizer();
        TokenStream result = new LowerCaseFilter(src);
        result = new StopFilter(result, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        result = new WordDelimiterGraphFilter(result, CATENATE_ALL, null);
//        result = new SynonymGraphFilter(result, generateSynonymMap(), true);
        result = new PatternReplaceFilter(result, Pattern.compile("[^a-zA-Z0-9]"), "", true);
        result = new TrimFilter(result);
        result = new PorterStemFilter(result);
//        result = new NGramTokenFilter(result, 4,4, true);
        return new TokenStreamComponents(src, result);

    }
}
