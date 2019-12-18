import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.*;

// https://www.baeldung.com/lucene-analyzers
public class MyCustomAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        StandardTokenizer src = new StandardTokenizer();
        TokenStream result = new StandardFilter(src);
        result = new LowerCaseFilter(result);
        result = new StopFilter(result,  StandardAnalyzer.ENGLISH_STOP_WORDS_SET);
        result = new PorterStemFilter(result);
        return new TokenStreamComponents(src, result);

    }
}