import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.spell.SuggestMode;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Helper {
    public static String queryTransform(String old, int sugs, Directory index, IndexReader reader, Analyzer analyzer)
            throws IOException {
        String[] words = old.strip().replaceAll("[^A-Za-z0-9 ]", "").split(" ");
        List<String> ors = new ArrayList<>();
        for(String word : words) {
            List<String> suggestions = new ArrayList<>(Arrays.asList(suggest(word, sugs, index, reader, analyzer)));
            if(!suggestions.contains(word)) {
                suggestions.add(word);
            }
            ors.add("(" + String.join(" OR ", suggestions) + ")");
        }
        return String.join(" AND ", ors);
    }

    public static String[] suggest(String word, int sugs, Directory index, IndexReader reader, Analyzer analyzer)
        throws IOException {
        SpellChecker sc = new SpellChecker(index);
        sc.indexDictionary(new LuceneDictionary(reader, "content"), new IndexWriterConfig(analyzer), false);
        return sc.suggestSimilar(word, sugs, reader, "content", SuggestMode.SUGGEST_ALWAYS);
    }
}
