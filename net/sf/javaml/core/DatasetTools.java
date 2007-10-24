/**
 * DatasetTools.java
 *
 * %SVN.HEADER%
 */
package net.sf.javaml.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

import net.sf.javaml.distance.DistanceMeasure;

/**
 * This class provides utility methods on datasets.
 * 
 * @author Thomas Abeel
 * 
 */
final public class DatasetTools {

    /**
     * Returns the instance of the given dataset that is closest to the instance
     * that is given as a parameter.
     * 
     * @param data
     *            the dataset to search in
     * @param inst
     *            the instance for which we need to find the closest
     * @return
     */
    public static Instance getClosest(Dataset data, DistanceMeasure dm, Instance inst) {
        Instance closest = data.getInstance(0);
        double bestDistance = dm.calculateDistance(inst, closest);
        for (int i = 1; i < data.size(); i++) {
            double tmpDistance = dm.calculateDistance(inst, data.getInstance(i));
            if (dm.compare(tmpDistance, bestDistance)) {
                bestDistance = tmpDistance;
                closest = data.getInstance(i);
            }
        }
        return closest;
    }

    /**
     * Return an instance with on each position the standard deviation for that
     * attribute.
     * 
     * @param data
     * @return
     */
    public static Instance getStandardDeviation(Dataset data) {
        int numAttributes = data.getInstance(0).size();
        double[] stdValues = new double[numAttributes];
        double[] attr = new double[data.size()];
        for (int i = 0; i < numAttributes; i++) {
            StandardDeviation std = new StandardDeviation();
            for (int j = 0; j < data.size(); j++) {
                attr[j] = data.getInstance(j).getValue(i);
            }
            stdValues[i] = std.evaluate(attr);
        }
        return new SimpleInstance(stdValues);

    }

    /**
     * Return the centroid of this cluster
     * 
     * @return the centroid of this dataset
     */
    public static Instance getCentroid(Dataset data) {
        if (data.size() == 0)
            return null;
        int instanceLength = data.getInstance(0).size();
        double[] sumPosition = new double[instanceLength];
        for (int i = 0; i < data.size(); i++) {
            Instance in = data.getInstance(i);
            for (int j = 0; j < instanceLength; j++) {

                sumPosition[j] += in.getWeight() * in.getValue(j);

            }

        }
        for (int j = 0; j < instanceLength; j++) {
            sumPosition[j] /= data.size();
        }
        return new SimpleInstance(sumPosition);

    }

    /**
     * Performs an epsilon range query for this instance relative to the
     * supplied dataset.
     * 
     * @param data
     *            the dataset on which to run the query.
     * @param epsilon
     *            Specifies the range for the query
     * @param instance
     *            The instance that is used as query-object for epsilon range
     *            query
     * @param dm
     *            The distance measure used to calculated the distances.
     * @return List with all the Instances that are within the specified range
     */
    public static List<Instance> epsilonRangeQuery(Dataset data, double epsilon, Instance instance, DistanceMeasure dm) {
        ArrayList<Instance> epsilonRange_List = new ArrayList<Instance>();

        for (int i = 0; i < data.size(); i++) {
            Instance tmp = data.getInstance(i);
            double distance = dm.calculateDistance(tmp, instance);
            if (distance < epsilon) {
                epsilonRange_List.add(tmp);
            }
        }

        return epsilonRange_List;
    }

    /**
     * Removes an instance from a dataset and returns the resulting dataset.
     * 
     * XXX DOC
     * 
     * @param data
     * @param index
     * @return
     */
    public static Dataset removeInstance(Dataset data, int index) {
        Dataset out = new SimpleDataset();
        for (int i = 0; i < data.size(); i++) {
            if (i != index)
                out.addInstance(data.getInstance(i));
        }
        return out;

    }

    /**
     * This method will merge all the data from the two datasets in to the first
     * one.
     * 
     * @param data
     * @param added
     */
    public static void merge(Dataset data, Dataset added) {
        for (int i = 0; i < added.size(); i++) {
            data.addInstance(added.getInstance(i));
        }
    }

    /**
     * Filter out all the empty datasets in an array.
     * 
     * XXX DOC
     * 
     * @param input
     * @return
     */
    public static Dataset[] filterEmpty(Dataset[] input) {
        // Filter empty clusters out;
        int nonEmptyClusterCount = 0;
        for (int i = 0; i < input.length; i++) {
            if (input[i].size() > 0)
                nonEmptyClusterCount++;
        }
        Dataset[] output = new Dataset[nonEmptyClusterCount];
        int index = 0;
        for (Dataset tmp : input) {
            if (tmp.size() > 0) {
                output[index] = tmp;
                index++;
            }
        }
        return output;
    }

}
