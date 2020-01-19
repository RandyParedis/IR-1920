import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.spell.SuggestMode;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.*;

public class Helper {
    private static Map<String, Set<Set<String>>> permutationsCache = new HashMap<>();

    public static String transform(String old) {
        return old.strip().toLowerCase().replaceAll("[^a-z0-9 ]", "");
    }

    public static String queryTransform(String old, int sugs, Directory index, IndexReader reader, Analyzer analyzer)
            throws IOException {
        String[] words = transform(old).split(" ");
        List<String> ors = new ArrayList<>();
        for(String word : words) {
            if(word.isEmpty()) { continue; }
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

    private static Set<Set<String>> permutations(Set<String> from) {
        int size = from.size();
        Set<Set<String>> result = new HashSet<>();
        if(size == 1) {
            result.add(from);
        } else {
            List<String> al = new ArrayList<>(from);
            Set<Set<String>> res = permutations(new TreeSet<>(al.subList(1, al.size())));
            result.add(new TreeSet<>(al.subList(0, 1)));
            result.addAll(res);
            for(Set<String> a: res) {
                List<String> b = new ArrayList<>(a);
                b.add(al.get(0));
                result.add(new TreeSet(b));
            }
        }
        return result;
    }

    public static Set<Set<String>> tagsToQueries(String tags) {
        if(permutationsCache.containsKey(tags)) {
            return permutationsCache.get(tags);
        } else {
            List<String> qs = Arrays.asList(tags.split("\\s+"));
            Set<String> from = new TreeSet<>(qs);
            Set<Set<String>> r = permutations(from);
            permutationsCache.put(tags, r);
            return r;
        }
    }

    public static void check(Object o1, Object o2) {
        if(!o1.equals(o2)) {
            throw new AssertionError(o1.toString() + " != " + o2.toString());
        }
    }
}
