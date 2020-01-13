import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Vector {
    private Map<String, Number> contents = new HashMap<>();

    public Vector() {}

    public Vector(Map<String, Number> vector) {
        contents = vector;
    }

    public boolean hasKey(String key) {
        return contents.containsKey(key);
    }

    public Number obtain(String name) {
        return contents.get(name);
    }

    public Set<Map.Entry<String, Number>> entrySet() {
        return contents.entrySet();
    }

    public int size() {
        return contents.size();
    }

    public void insert(String name, Number value) {
        contents.put(name, value);
    }

    public void insertAll(Vector vector) {
        contents.putAll(vector.contents);
    }

    public void insertAll(Map<String, Number> vector) {
        contents.putAll(vector);
    }

    public Vector add(Vector other) {
        Vector n = new Vector();
        for(Map.Entry<String, Number> entry: other.contents.entrySet()) {
            String key = entry.getKey();
            if(!n.hasKey(key)) {
                n.insert(key, entry.getValue());
            } else {
                n.insert(key, n.obtain(key).doubleValue() + entry.getValue().doubleValue());
            }
        }
        return n;
    }

    public Vector add(Number other) {
        Vector n = new Vector();
        for(Map.Entry<String, Number> e: entrySet()) {
            n.insert(e.getKey(), e.getValue().doubleValue() + other.doubleValue());
        }
        return n;
    }

    public void addFor(String term, Number value) {
        if(!hasKey(term)) {
            insert(term, value);
        } else {
            insert(term, obtain(term).doubleValue() + value.doubleValue());
        }
    }

    public Vector subtract(Vector other) {
        Vector n = new Vector();
        for(Map.Entry<String, Number> entry: other.contents.entrySet()) {
            String key = entry.getKey();
            if(!n.hasKey(key)) {
                n.insert(key, - entry.getValue().doubleValue());
            } else {
                n.insert(key, n.obtain(key).doubleValue() - entry.getValue().doubleValue());
            }
        }
        return n;
    }

    public Vector subtract(Number other) {
        Vector n = new Vector();
        for(Map.Entry<String, Number> e: entrySet()) {
            n.insert(e.getKey(), e.getValue().doubleValue() - other.doubleValue());
        }
        return n;
    }

    public Vector multiply(Number other) {
        Vector n = new Vector();
        for(Map.Entry<String, Number> e: entrySet()) {
            n.insert(e.getKey(), e.getValue().doubleValue() * other.doubleValue());
        }
        return n;
    }

    public Vector divide(Number other) {
        Vector n = new Vector();
        for(Map.Entry<String, Number> e: entrySet()) {
            n.insert(e.getKey(), e.getValue().doubleValue() / other.doubleValue());
        }
        return n;
    }

    public Vector inverse() {
        Vector n = new Vector();
        for(Map.Entry<String, Number> e: entrySet()) {
            n.insert(e.getKey(), 1.0 / e.getValue().doubleValue());
        }
        return n;
    }

    public Vector mean() {
        return divide(size());
    }

    public Number sum() {
        Number num = 0.0d;
        for(Map.Entry<String, Number> entry: entrySet()) {
            num = num.doubleValue() + entry.getValue().doubleValue();
        }
        return num;
    }

    public Vector normalize() {
        return divide(sum());
    }
}
