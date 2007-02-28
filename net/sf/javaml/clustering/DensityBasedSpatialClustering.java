/**
 * DensityBasedSpatialClustering.java
 *
 * This file is part of the Java Machine Learning API
 * 
 * The Java Machine Learning API is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * The Java Machine Learning API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Java Machine Learning API; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * Copyright (c) 2006-2007, Thomas Abeel
 * 
 * Project: http://sourceforge.net/projects/java-ml/
 * 
 */
package net.sf.javaml.clustering;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DatasetTools;
import net.sf.javaml.core.Instance;
import net.sf.javaml.core.SimpleDataset;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.distance.NormalizedEuclideanDistance;

public class DensityBasedSpatialClustering implements Clusterer {

    private DistanceMeasure dm;

    /**
     * Specifies the radius for a range-query
     */
    private double epsilon = 0.01;

    /**
     * Specifies the density (the range-query must contain at least minPoints
     * instances)
     */
    private int minPoints = 6;

    /**
     * Holds the current clusterID
     */
    private int clusterID;

    public DensityBasedSpatialClustering(){
        
    }
    
    public DensityBasedSpatialClustering(double epsilon, int minPoints, DistanceMeasure dm) {
        this.dm = dm;
        this.epsilon = epsilon;
        this.minPoints = minPoints;
    }

    private List<DataObject> epsilonRangeQuery(double epsilon, Instance inst) {
        List<Instance> tmp = DatasetTools.epsilonRangeQuery(originalData, epsilon, inst, dm);
        List<DataObject> out = new LinkedList<DataObject>();
        for (Instance i : tmp) {
            out.add(new DataObject(i));
        }
        return out;
    }

    /**
     * Assigns this dataObject to a cluster or remains it as NOISE
     * 
     * @param instance
     *            The DataObject that needs to be assigned
     * @return true, if the DataObject could be assigned, else false
     */
    private boolean expandCluster(DataObject dataObject) {
        List<DataObject> seedList = epsilonRangeQuery(epsilon, dataObject.instance);

        System.out.println("Created initial seedlist with "+seedList.size()+" nodes");
        /** dataObject is NO coreObject */
        if (seedList.size() < minPoints) {
            dataObject.clusterIndex = DataObject.NOISE;
            return false;
        }

        System.out.println("Object is core object");
        /** dataObject is coreObject */
        for (int i = 0; i < seedList.size(); i++) {
            DataObject seedListDataObject = seedList.get(i);
            /**
             * label this seedListDataObject with the current clusterID, because
             * it is in epsilon-range
             */
            seedListDataObject.clusterIndex = clusterID;
            if (seedListDataObject.equals(dataObject)) {
                seedList.remove(i);
                i--;
            }
        }

        System.out.println("Seedlist is labeled and pruned");
        /** Iterate the seedList of the startDataObject */
        for (int j = 0; j < seedList.size(); j++) {
            System.out.println("Add neighbours, seedList size: "+seedList.size());
            if(seedList.size()>10000)
                System.exit(-1);
            DataObject seedListDataObject = seedList.get(j);
            List<DataObject> seedListDataObject_Neighbourhood = epsilonRangeQuery(epsilon, seedListDataObject.instance);

            /** seedListDataObject is coreObject */
            if (seedListDataObject_Neighbourhood.size() >= minPoints) {
                for (int i = 0; i < seedListDataObject_Neighbourhood.size(); i++) {
                    DataObject p = seedListDataObject_Neighbourhood.get(i);
                    if (p.clusterIndex == DataObject.UNCLASSIFIED || p.clusterIndex == DataObject.NOISE) {
                        if (p.clusterIndex == DataObject.UNCLASSIFIED) {
                            seedList.add(p);
                        }
                        p.clusterIndex = clusterID;
                    }
                }
            }
            seedList.remove(j);
            j--;
        }

        return true;
    }

    class DataObject {
        int clusterIndex = -1;

        static final int UNCLASSIFIED = -1;

        static final int NOISE = -2;

        Instance instance;

        public DataObject(Instance inst) {
            this.instance = inst;
        }
    }

    private Vector<DataObject> dataset = null;

    private Dataset originalData = null;

    public Dataset[] executeClustering(Dataset data) {
        this.originalData = data;
        this.dm=new NormalizedEuclideanDistance(this.originalData);
        this.clusterID = 0;
        this.dataset = new Vector<DataObject>();
        for (int i = 0; i < data.size(); i++) {
            dataset.add(new DataObject(data.getInstance(i)));

        }

        for (DataObject dataObject : dataset) {
            if (dataObject.clusterIndex == DataObject.UNCLASSIFIED) {
                System.out.println("Starting to expand...");
                if (expandCluster(dataObject)) {
                    System.out.println(clusterID);
                    clusterID++;

                }
            }
        }

        Dataset[] clusters = new Dataset[clusterID];
        for (int i = 0; i < clusters.length; i++) {
            clusters[i] = new SimpleDataset();
        }
        for (DataObject dataObject : dataset) {
            clusters[dataObject.clusterIndex].addInstance(dataObject.instance);
        }
        return clusters;
    }

}