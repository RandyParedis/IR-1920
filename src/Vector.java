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

    public Vector copy() {
        return new Vector(this.contents);
    }

    public Vector add(Vector other) {
        for(Map.Entry<String, Number> entry: other.contents.entrySet()) {
            String key = entry.getKey();
            addFor(key, entry.getValue());
        }
        return this;
    }

    public Vector add(Number other) {
        for(Map.Entry<String, Number> e: entrySet()) {
            insert(e.getKey(), e.getValue().doubleValue() + other.doubleValue());
        }
        return this;
    }

    public void addFor(String term, Number value) {
        if(!hasKey(term)) {
            insert(term, value);
        } else {
            insert(term, obtain(term).doubleValue() + value.doubleValue());
        }
    }

    public Vector subtract(Vector other) {
        for(Map.Entry<String, Number> entry: other.contents.entrySet()) {
            String key = entry.getKey();
            if(!hasKey(key)) {
                insert(key, - entry.getValue().doubleValue());
            } else {
                insert(key, obtain(key).doubleValue() - entry.getValue().doubleValue());
            }
        }
        return this;
    }

    public Vector subtract(Number other) {
        for(Map.Entry<String, Number> e: entrySet()) {
            insert(e.getKey(), e.getValue().doubleValue() - other.doubleValue());
        }
        return this;
    }

    public Vector multiply(Number other) {
        for(Map.Entry<String, Number> e: entrySet()) {
            insert(e.getKey(), e.getValue().doubleValue() * other.doubleValue());
        }
        return this;
    }

    public Vector divide(Number other) {
        for(Map.Entry<String, Number> e: entrySet()) {
            insert(e.getKey(), e.getValue().doubleValue() / other.doubleValue());
        }
        return this;
    }

    public Vector inverse() {
        for(Map.Entry<String, Number> e: entrySet()) {
            insert(e.getKey(), 1.0 / e.getValue().doubleValue());
        }
        return this;
    }

    public Vector mean() {
        Vector vec = copy();
        return vec.divide(vec.size());
    }

    public Number sum() {
        Number num = 0.0d;
        for(Map.Entry<String, Number> entry: entrySet()) {
            num = num.doubleValue() + entry.getValue().doubleValue();
        }
        return num;
    }

    public Number min() {
        Number min = Double.POSITIVE_INFINITY;
        for(Map.Entry<String, Number> entry: entrySet()) {
            Number val = entry.getValue();
            if(entry.getValue().doubleValue() < min.doubleValue()) {
                min = val;
            }
        }
        return min;
    }

    public Number max() {
        Number max = Double.NEGATIVE_INFINITY;
        for(Map.Entry<String, Number> entry: entrySet()) {
            Number val = entry.getValue();
            if(entry.getValue().doubleValue() > max.doubleValue()) {
                max = val;
            }
        }
        return max;
    }

    public Vector normalize() {
        return divide(sum());
    }

    public Vector normalized() {
        Vector vec = copy();
        return vec.divide(vec.sum());
    }
}
