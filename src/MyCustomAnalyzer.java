import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;

// https://www.baeldung.com/lucene-analyzers
public class MyCustomAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        StandardTokenizer src = new StandardTokenizer();
        TokenStream result = new LowerCaseFilter(src);
        result = new StopFilter(result, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        result = new PorterStemFilter(result);
        return new TokenStreamComponents(src, result);

    }
}