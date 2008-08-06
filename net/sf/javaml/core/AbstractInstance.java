/**
 * AbstractInstance.java
 *
 * %SVN.HEADER%
 */
package net.sf.javaml.core;

import java.util.Iterator;

/**
 * Implementation of some standard methods for instances.
 * 
 * {@jmlSource}
 * 
 * @see Instance
 * 
 * @version %SVN.REVISION%
 * 
 * @author Thomas Abeel
 * 
 */
public abstract class AbstractInstance implements Instance {
    static int nextID = 0;

    private final int ID;

    public int getID(){
        return ID;
    }
    class InstanceValueIterator implements Iterator<Double> {

        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < noAttributes() - 1;
        }

        @Override
        public Double next() {
            index++;
            return value(index - 1);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Cannot remove from instance using the iterator.");

        }

    }

    @Override
    public Iterator<Double> iterator() {
        return new InstanceValueIterator();
    }

    private Object classValue;

    protected AbstractInstance() {
        this(null);
    }

    protected AbstractInstance(Object classValue) {
        ID = nextID;
        nextID++;
        this.classValue = classValue;
    }

    @Override
    public Object classValue() {
        return classValue;
    }

    @Override
    public void setClassValue(Object classValue) {
        this.classValue = classValue;
    }

    @Override
    public Instance minus(Instance min) {
        Instance out = new DenseInstance(new double[this.noAttributes()]);
        for (int i = 0; i < this.noAttributes(); i++) {
            out.put(i, this.get(i) - min.get(i));
        }
        return out;

    }

    @Override
    public Instance minus(double min) {
        Instance out = new DenseInstance(new double[this.noAttributes()]);
        for (int i = 0; i < this.noAttributes(); i++) {
            out.put(i, this.get(i) - min);
        }
        return out;

    }

    @Override
    public Instance divide(double min) {
        Instance out = new DenseInstance(new double[this.noAttributes()]);
        for (int i = 0; i < this.noAttributes(); i++) {
            out.put(i, this.get(i) / min);
        }
        return out;
    }

    @Override
    public Instance multiply(double value) {
        Instance out = new DenseInstance(new double[this.noAttributes()]);
        for (int i = 0; i < this.noAttributes(); i++) {
            out.put(i, this.get(i) * value);
        }
        return out;
    }

    @Override
    public Instance multiply(Instance value) {
        Instance out = new DenseInstance(new double[this.noAttributes()]);
        for (int i = 0; i < this.noAttributes(); i++) {
            out.put(i, this.get(i) * value.get(i));
        }
        return out;
    }

    @Override
    public Instance divide(Instance min) {
        Instance out = new DenseInstance(new double[this.noAttributes()]);
        for (int i = 0; i < this.noAttributes(); i++) {
            out.put(i, this.get(i) / min.get(i));
        }
        return out;
    }

    @Override
    public Instance plus(double min) {
        Instance out = new DenseInstance(new double[this.noAttributes()]);
        for (int i = 0; i < this.noAttributes(); i++) {
            out.put(i, this.get(i) + min);
        }
        return out;
    }

    @Override
    public Instance plus(Instance min) {
        Instance out = new DenseInstance(new double[this.noAttributes()]);
        for (int i = 0; i < this.noAttributes(); i++) {
            out.put(i, this.get(i) + min.get(i));
        }
        return out;
    }

    @Override
    public Instance sqrt() {
        Instance out = new DenseInstance(new double[this.noAttributes()]);
        for (int i = 0; i < this.noAttributes(); i++) {
            out.put(i, Math.sqrt(this.get(i)));
        }
        return out;
    }
}