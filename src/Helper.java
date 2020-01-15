import jdk.jshell.spi.ExecutionControl.NotImplementedException;
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

    private static String toJSONMap(Map obj, int d) throws NotImplementedException {
        String res = "{";
        Map<Object, Object> m = (Map<Object, Object>)(obj);
        List<String> s = new ArrayList<>();
        for(Map.Entry<Object, Object> entry: m.entrySet()) {
            String key = "\"" + entry.getKey().toString() + "\"";
            String kv = key + ": " + toJSON(entry.getValue(), d+1);
            s.add(kv);
        }
        res += "\n" + ("\t".repeat(d+1)) + String.join(",\n" + ("\t".repeat(d+1)), s);
        res += "\n" + ("\t".repeat(d)) + "}";
        return res;
    }

    private static String toJSONList(List obj, int d) throws NotImplementedException {
        String res = "[";
        ArrayList<String> s = new ArrayList<>();
        boolean p = false;
        boolean n = false;
        for(Object o: obj) {
            if(List.class.isAssignableFrom(o.getClass())) {
                s.add("\n" + "\t".repeat(d+1) + toJSON(o, d+1));
                p = n = true;
            } else {
                String g = "";
                if(p) {
                    g = "\n" + "\t".repeat(d);
                }
                s.add(g + toJSON(o, d+1));
                p = false;
            }
        }
        res += String.join(", ", s);
        if(n) {
            res += "\n" + "\t".repeat(d);
        }
        res += "]";
        return res;
    }

    private static String toJSON(Object obj, int depth) throws NotImplementedException {
        String res = "";
        if(Map.class.isAssignableFrom(obj.getClass()))  {
            res += toJSONMap((Map)obj, depth);
        } else if(List.class.isAssignableFrom(obj.getClass())) {
            res += toJSONList((List)obj, depth);
        } else if(String.class.isAssignableFrom(obj.getClass())) {
            res += "\"" + obj + "\"";
        } else if(Number.class.isAssignableFrom(obj.getClass())) {
            res += "" + ((Number)obj).floatValue();
        } else {
            throw new NotImplementedException("Invalid object class '" + obj.getClass() + "'");
        }
        return res;
    }

    public static String toJSON(Object obj) throws NotImplementedException {
        return toJSON(obj, 0);
    }

    public static void check(Object o1, Object o2) {
        if(!o1.equals(o2)) {
            throw new AssertionError(o1.toString() + " != " + o2.toString());
        }
    }
}
