import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.synonym.SynonymFilter;

import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.CATENATE_ALL;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.util.CharsRef;

// https://www.baeldung.com/lucene-analyzers
public class MyCustomAnalyzer extends Analyzer {
    private SynonymMap generateSynonymMap() {
        BufferedReader reader;
        try {
            SynonymMap.Builder builder = new SynonymMap.Builder(true);
            reader = new BufferedReader(new FileReader("data/en_thesaurus_synonyms.txt"));
            String line = reader.readLine();
            while (line != null) {
                JSONParser parser = new JSONParser();
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
        result = new PorterStemFilter(result);
        result = new SynonymFilter(result, generateSynonymMap(), true);
        result = new NGramTokenFilter(result, 4);
        return new TokenStreamComponents(src, result);

    }
}