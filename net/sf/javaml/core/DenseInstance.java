package net.sf.javaml.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DenseInstance extends AbstractInstance implements Instance {

    private double[] attributes;

    // public DenseInstance(int noAttributes) {
    // this.attributes = new double[noAttributes];
    // }

    public DenseInstance(double[] att) {
        this(att, null);
    }

    public DenseInstance(double[] att, Object classValue) {
        super(classValue);
        this.attributes = att.clone();
//        this.classValue = classValue;
    }

//    private Object classValue = null;

//    @Override
//    public Object classValue() {
//        return classValue;
//    }

    // @Override
    // public int noAttributes() {
    // return attributes.length;
    // }

    @Override
    public double value(int pos) {
        return attributes[pos];
    }

    @Override
    public void clear() {
        attributes = new double[attributes.length];

    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof Integer) {
            int i = (Integer) key;
            return i >= 0 && i < attributes.length;
        } else
            return false;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value instanceof Number) {
            double val = ((Number) value).doubleValue();
            for (int i = 0; i < attributes.length; i++) {
                if (Math.abs(val - attributes[i]) < 0.00000001)
                    return true;
            }
        }
        return false;
    }

    @Override
    public Set<java.util.Map.Entry<Integer, Double>> entrySet() {
        HashMap<Integer, Double> map = new HashMap<Integer, Double>();
        for (int i = 0; i < attributes.length; i++)
            map.put(i, attributes[i]);
        return map.entrySet();
    }

    @Override
    public Double get(Object key) {
        return attributes[(Integer) key];
    }

    @Override
    public boolean isEmpty() {

        return false;
    }

    @Override
    public Set<Integer> keySet() {
        HashSet<Integer> keys = new HashSet<Integer>();
        for (int i = 0; i < attributes.length; i++)
            keys.add(i);
        return keys;
    }

    @Override
    public Double put(Integer key, Double value) {
        double val = attributes[key];
        attributes[key] = value;
        return val;

    }

    @Override
    public void putAll(Map<? extends Integer, ? extends Double> m) {
        for (Integer key : m.keySet()) {
            attributes[key] = m.get(key);
        }

    }

    @Override
    public Double remove(Object key) {
        throw new UnsupportedOperationException("Cannot remove attributes from a dense instance.");
    }

    @Override
    public int size() {
        return attributes.length;
    }

    @Override
    public Collection<Double> values() {
        Collection<Double> vals = new ArrayList<Double>();
        for (double v : attributes)
            vals.add(v);
        return vals;
    }

    @Override
    public int noAttributes() {
        return attributes.length;
    }

    @Override
    public String toString() {
        return "{" + Arrays.toString(attributes) + ";" + classValue() + "}";
    }

}
