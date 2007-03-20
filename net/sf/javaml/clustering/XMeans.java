/**
 * XMeans.java
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
 * Copyright (c) 2000 Mark Hall, Malcolm Ware, Gabi Schmidberger
 * Copyright (c) 2006-2007, Thomas Abeel
 * 
 * 
 * Project: http://sourceforge.net/projects/java-ml/
 * 
 */
package net.sf.javaml.clustering;

import java.text.NumberFormat;
import java.util.Random;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.core.KDTree;
import net.sf.javaml.core.SimpleDataset;
import net.sf.javaml.core.SimpleInstance;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.distance.EuclideanDistance;
import net.sf.javaml.utils.ArrayOperations;
import net.sf.javaml.utils.MathUtils;

/**
 * Cluster data using the X-means algorithm.<br/> <br/> X-Means is K-Means
 * extended by an Improve-Structure part In this part of the algorithm the
 * centers are attempted to be split in its region. The decision between the
 * children of each center and itself is done comparing the BIC-values of the
 * two structures.<br/> <br/> For more information see:<br/> <br/> Dan Pelleg,
 * Andrew W. Moore: X-means: Extending K-means with Efficient Estimation of the
 * Number of Clusters. In: Seventeenth International Conference on Machine
 * Learning, 727-734, 2000. <p/>
 * 
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author Malcolm Ware (mfw4@cs.waikato.ac.nz)
 * @author Thomas Abeel
 * 
 */
public class XMeans implements Clusterer {

    private Dataset data = null;

    // private Dataset model = null;

    /** BIC-Score of the current model */
    private double bic = Double.MIN_VALUE;

    /** Distortion */
    private double[] m_Mle = null;

    /** maximum overall iterations */
    private int m_MaxIterations = 5;

    /**
     * maximum iterations to perform Kmeans part if negative, iterations are not
     * checked
     */
    private int m_MaxKMeans = 1000;

    /**
     * see above, but for kMeans of splitted clusters
     */
    private int m_MaxKMeansForChildren = 1000;

    /** The actual number of clusters */
    private int m_NumClusters = 2;

    /** min number of clusters to generate */
    private int m_MinNumClusters = 2;

    /** max number of clusters to generate */
    private int m_MaxNumClusters = 10;

    /** the distance function used */
    private DistanceMeasure dm = new EuclideanDistance();

    /** cluster centers */
    private Instance[] m_ClusterCenters;

    /**
     * temporary variable holding cluster assignments while iterating
     */
    private int[] m_ClusterAssignments;

    /**
     * cutoff factor - percentage of splits done in Improve-Structure part only
     * relevant, if all children lost
     */
    private double m_CutOffFactor = 0.5;

    /**
     * KDTrees class if KDTrees are used
     */
    private KDTree m_KDTree = new KDTree();

    /**
     * whether to use the KDTree (the KDTree is only initialized to be
     * configurable from the GUI)
     */
    private boolean m_UseKDTree = true;

    /** counts iterations done in main loop */
    private int m_IterationCount = 0;

    /** counter to say how often kMeans was stopped by loop counter */
    private int m_KMeansStopped = 0;

    /** Number of splits prepared */
    private int m_NumSplits = 0;

    /** Number of splits accepted (including cutoff factor decisions) */
    private int m_NumSplitsDone = 0;

    /** Number of splits accepted just because of cutoff factor */
    private int m_NumSplitsStillDone = 0;

    /**
     * Generates the X-Means clusterer.
     * 
     * @param data
     *            set of instances serving as training data
     * @throws Exception
     *             if the clusterer has not been generated successfully
     */
    public Dataset[] executeClustering(Dataset data) {
        this.data = data;

        m_NumSplits = 0;
        m_NumSplitsDone = 0;
        m_NumSplitsStillDone = 0;

        // initialize random function
        Random rg = new Random(System.currentTimeMillis());

        // num of clusters to start with
        m_NumClusters = m_MinNumClusters;

        // checkInstances();
        //
        // if (m_DebugVectorsFile.exists() && m_DebugVectorsFile.isFile())
        // initDebugVectorsInput();

        // make list of indexes for m_Instances
        int[] allInstList = new int[data.size()];
        for (int i = 0; i < data.size(); i++) {
            allInstList[i] = i;
        }

        // // set model used (just for convenience)
        // model = new SimpleDataset();

        // makes the first centers randomly
        m_ClusterCenters = makeCentersRandomly(rg, data, m_NumClusters);

        boolean finished = false;
        Instance[] children;

        // builds up a KDTree
        if (m_UseKDTree)
            m_KDTree.setInstances(data);

        // loop counter of main loop
        m_IterationCount = 0;

        /**
         * "finished" does get true as soon as: 1. number of clusters gets >=
         * m_MaxClusters, 2. in the last round, none of the centers have been
         * split
         * 
         * if number of clusters is already >= m_MaxClusters part 1 (=
         * Improve-Params) is done at least once.
         */
        while (!finished && !stopIteration(m_IterationCount, m_MaxIterations)) {

            /*
             * ====================================================================
             * 1. Improve-Params conventional K-means
             */

            m_IterationCount++;

            // prepare to converge
            boolean converged = false;

            // initialize assignments to -1
            m_ClusterAssignments = initAssignments(data.size());
            // stores a list of indexes of instances belonging to each center
            int[][] instOfCent = new int[m_ClusterCenters.length][];

            // KMeans loop counter
            int kMeansIteration = 0;

            // converge in conventional K-means
            // ----------------------------------
            //PFD(0, "\nConverge in K-Means:");
            while (!converged && !stopKMeansIteration(kMeansIteration, m_MaxKMeans)) {

                kMeansIteration++;
                converged = true;

                // assign instances to centers
                // -------------------------------------
                converged = assignToCenters(m_UseKDTree ? m_KDTree : null, m_ClusterCenters, instOfCent, allInstList,
                        m_ClusterAssignments, kMeansIteration);

                // compute new centers = centers of mass of points
                converged = recomputeCenters(m_ClusterCenters, instOfCent); // model
                // information

                // PFD(0, "\nMain loop - Recompute - centers:");
                // PrCentersFD(0);
            }

            /**
             * ===================== 2. Improve-Structure =====================
             */

            // BIC before split distortioning the centres
            m_Mle = distortion(instOfCent, m_ClusterCenters);
            bic = calculateBIC(instOfCent, m_ClusterCenters, m_Mle);
           // PFD(0, "m_Bic " + bic);

            int currNumCent = m_ClusterCenters.length;
            Instance[] splitCenters = new Instance[currNumCent * 2];// (m_ClusterCenters,
            // currNumCent
            // * 2);
            int splitIndex = 0;
            // store BIC values of parent and children
            double[] pbic = new double[currNumCent];
            double[] cbic = new double[currNumCent];

            // split each center
            for (int i = 0; i < currNumCent
            // this could help to optimize the algorithm
            // && currNumCent + numSplits <= m_MaxNumClusters
            ; i++) {

                //PFD(0, "\nsplit center " + i + " " + m_ClusterCenters[i]);
                Instance currCenter = m_ClusterCenters[i];
                int[] currInstList = instOfCent[i];
                int currNumInst = instOfCent[i].length;

                // not enough instances; than continue with next
                if (currNumInst <= 2) {
                    pbic[i] = Double.MAX_VALUE;
                    cbic[i] = 0.0;
                    // add center itself as dummy
                    splitCenters[splitIndex++] = currCenter;
                    splitCenters[splitIndex++] = currCenter;
                    continue;
                }

                // split centers ----------------------------------------------
                double variance = m_Mle[i] / (double) currNumInst;
                children = splitCenter(rg, currCenter, variance);
//                System.out.println("Preparing split: ");
//                System.out.println("\tChild 1: " + children[0]);
//                System.out.println("\tChild 2: " + children[1]);
                // initialize assignments to -1
                int[] oneCentAssignments = initAssignments(currNumInst);
                int[][] instOfChCent = new int[2][]; // todo maybe split
                // didn't work

                // converge the children --------------------------------------
                converged = false;
                int kMeansForChildrenIteration = 0;
                //PFD(0, "\nConverge, K-Means for children: " + i);
                while (!converged && !stopKMeansIteration(kMeansForChildrenIteration, m_MaxKMeansForChildren)) {
                    kMeansForChildrenIteration++;
                    converged = assignToCenters(children, instOfChCent, currInstList, oneCentAssignments);
                    if (!converged) {
                        recomputeCentersFast(children, instOfChCent);
                    }
                }

                // store new centers for later decision if they are taken
                splitCenters[splitIndex++] = children[0];// .instance(0));
                splitCenters[splitIndex++] = children[1];// .instance(1));

//                PFD(0, "\nconverged children ");
//                PFD(0, " " + children[0]);
//                PFD(0, " " + children[1]);

                // compare parent and children model by their BIC-value
                pbic[i] = calculateBIC(currInstList, currCenter, m_Mle[i]);
                double[] chMLE = distortion(instOfChCent, children);
                cbic[i] = calculateBIC(instOfChCent, children, chMLE);

            } // end of loop over clusters

            // decide which one to split and make new list of cluster centers
            Instance[] newClusterCenters = null;
            newClusterCenters = newCentersAfterSplit(pbic, cbic, m_CutOffFactor, splitCenters);
            /**
             * Compare with before Improve-Structure
             */
            int newNumClusters = newClusterCenters.length;
            if (newNumClusters != m_NumClusters) {

              //  PFD(0, "Compare with non-split");
//
                // initialize assignments to -1
                int[] newClusterAssignments = initAssignments(data.size());

                // stores a list of indexes of instances belonging to each
                // center
                int[][] newInstOfCent = new int[newClusterCenters.length][];

                // assign instances to centers
                // -------------------------------------
                converged = assignToCenters(m_UseKDTree ? m_KDTree : null, newClusterCenters, newInstOfCent,
                        allInstList, newClusterAssignments, m_IterationCount);

                double[] newMle = distortion(newInstOfCent, newClusterCenters);
                double newBic = calculateBIC(newInstOfCent, newClusterCenters, newMle);
               // PFD(0, "newBic " + newBic);
                if (newBic > bic) {
                   // PFD(0, "*** decide for new clusters");
                    bic = newBic;
                    m_ClusterCenters = newClusterCenters;
                    m_ClusterAssignments = newClusterAssignments;
                } else {
                   // PFD(0, "*** keep old clusters");
                }
            }

            newNumClusters = m_ClusterCenters.length;
            // decide if finished: max num cluster reached
            // or last centers where not split at all
            if ((newNumClusters >= m_MaxNumClusters) || (newNumClusters == m_NumClusters)) {
                finished = true;
            }
            m_NumClusters = newNumClusters;
        }
        Dataset[] out = new Dataset[m_NumClusters];
        for (int i = 0; i < out.length; i++) {
            out[i] = new SimpleDataset();
        }
        for (int i = 0; i < data.size(); i++) {
            Instance inst = data.getInstance(i);
            int index = clusterProcessedInstance(inst, m_ClusterCenters);
            out[index].addInstance(inst);
        }
       // System.out.println(this.toString());
        return out;
    }

    /**
     * Creates and initializes integer array, used to store assignments.
     * 
     * @param numInstances
     *            length of array used for assignments
     * @return integer array used for storing assignments
     */
    private int[] initAssignments(int numInstances) {
        int[] ass = new int[numInstances];
        for (int i = 0; i < numInstances; i++)
            ass[i] = -1;
        return ass;
    }

    /**
     * Returns new center list.
     * 
     * The following steps 1. and 2. both take care that the number of centers
     * does not exceed maxCenters.
     * 
     * 1. Compare BIC values of parent and children and takes the one as new
     * centers which do win (= BIC-value is smaller).
     * 
     * 2. If in 1. none of the children are chosen && and cutoff factor is > 0
     * cutoff factor is taken as the percentage of "best" centers that are still
     * taken.
     * 
     * @param pbic
     *            array of parents BIC-values
     * @param cbic
     *            array of childrens BIC-values
     * @param cutoffFactor
     *            cutoff factor
     * @param splitCenters
     *            all children
     * @return the new centers
     */
    private Instance[] newCentersAfterSplit(double[] pbic, double[] cbic, double cutoffFactor, Instance[] splitCenters) {

        // store if split won
        boolean splitPerCutoff = false;
        boolean takeSomeAway = false;
        boolean[] splitWon = new boolean[m_ClusterCenters.length];
        int numToSplit = 0;
        Instance[] newCenters = null;

        // how many would be split, because the children have a better bic value
        for (int i = 0; i < cbic.length; i++) {
            if (cbic[i] > pbic[i]) {
                // decide for splitting ----------------------------------------
                splitWon[i] = true;
                numToSplit++;
                // PFD(0, "Center " + i + " decide for children");
            } else {
                // decide for parents and finished stays true -----------------
                // PFD(0, "Center " + i + " decide for parent");
            }
        }

        // no splits yet so split per cutoff factor
        if ((numToSplit == 0) && (cutoffFactor > 0)) {
            splitPerCutoff = true;

            // how many to split per cutoff factor
            numToSplit = (int) ((double) m_ClusterCenters.length * m_CutOffFactor);
        }

        // prepare indexes of values in ascending order
        double[] diff = new double[m_NumClusters];
        for (int j = 0; j < diff.length; j++) {
            diff[j] = pbic[j] - cbic[j];
        }
        int[] sortOrder = ArrayOperations.sort(diff);

        // check if maxNumClusters would be exceeded
        int possibleToSplit = m_MaxNumClusters - m_NumClusters;

        if (possibleToSplit > numToSplit) {
            // still enough possible, do the whole amount
            possibleToSplit = numToSplit;
        } else
            takeSomeAway = true;

        // prepare for splitting the one that are supposed to be split
        if (splitPerCutoff) {
            for (int j = 0; (j < possibleToSplit) && (cbic[sortOrder[j]] > 0.0); j++) {
                splitWon[sortOrder[j]] = true;
            }
            m_NumSplitsStillDone += possibleToSplit;
        } else {
            // take some splits away if max number of clusters would be exceeded
            if (takeSomeAway) {
                int count = 0;
                int j = 0;
                for (; j < splitWon.length && count < possibleToSplit; j++) {
                    if (splitWon[sortOrder[j]] == true)
                        count++;
                }

                while (j < splitWon.length) {
                    splitWon[sortOrder[j]] = false;
                    j++;
                }
            }
        }

        // finally split
        if (possibleToSplit > 0)
            newCenters = newCentersAfterSplit(splitWon, splitCenters);
        else
            newCenters = m_ClusterCenters;
        return newCenters;
    }

    /**
     * Returns new centers. Depending on splitWon: if true takes children, if
     * false takes parent = current center.
     * 
     * @param splitWon
     *            array of boolean to indicate to take split or not
     * @param splitCenters
     *            list of splitted centers
     * @return the new centers
     */
    private Instance[] newCentersAfterSplit(boolean[] splitWon, Instance[] splitCenters) {

        int countTrue = 0, countFalse = 0;
        for (int i = 0; i < splitWon.length; i++) {
            if (splitWon[i]) {
                countTrue++;
            } else {
                countFalse++;
            }
        }
        Instance[] newCenters = new Instance[2 * countTrue + countFalse];
        int sIndex = 0;
        int index = 0;
        for (int i = 0; i < splitWon.length; i++) {
            if (splitWon[i]) {
                m_NumSplitsDone++;
                newCenters[index] = splitCenters[sIndex++];
                index++;
                newCenters[index] = splitCenters[sIndex++];
                index++;
            } else {
                sIndex++;
                sIndex++;
                newCenters[index] = m_ClusterCenters[i];
                index++;
            }
        }
        return newCenters;
    }

    /**
     * Controls that counter does not exceed max iteration value. Special
     * function for kmeans iterations.
     * 
     * @param iterationCount
     *            current value of counter
     * @param max
     *            maximum value for counter
     * @return true if iteration should be stopped
     */
    private boolean stopKMeansIteration(int iterationCount, int max) {
        boolean stopIterate = false;
        if (max >= 0)
            stopIterate = (iterationCount >= max);
        if (stopIterate)
            m_KMeansStopped++;
        return stopIterate;
    }

    /**
     * Checks if iterationCount has to be checked and if yes (this means max is >
     * 0) compares it with max
     * 
     * @param iterationCount
     *            the current iteration count
     * @param max
     *            the maximum number of iterations
     * @return true if maximum has been reached
     */
    private boolean stopIteration(int iterationCount, int max) {
        boolean stopIterate = false;
        if (max >= 0)
            stopIterate = (iterationCount >= max);
        return stopIterate;
    }

    /**
     * Recompute the new centers. New cluster center is center of mass of its
     * instances. Returns true if cluster stays the same.
     * 
     * @param centers
     *            the input and output centers
     * @param instOfCent
     *            the instances to the centers
     * @param model
     *            data model information
     * @return true if converged.
     */
    // USED BY KMEANS
    private boolean recomputeCenters(Instance[] centers, int[][] instOfCent) {
        boolean converged = true;

        for (int i = 0; i < centers.length; i++) {
            double val;
            float[] centerValues = centers[i].toArray();
            for (int j = 0; j < centers[i].size(); j++) {

                val = meanOrMode(data, instOfCent[i], j);

                for (int k = 0; k < instOfCent[i].length; k++)

                    if (converged && m_ClusterCenters[i].getValue(j) != val)
                        converged = false;
                if (!converged) {
                    // m_ClusterCenters[i].setValue(j, val);
                    centerValues[j] = (float) val;
                }
            }
            m_ClusterCenters[i] = new SimpleInstance(centerValues, centers[i].getWeight(), centers[i].isClassSet(),
                    centers[i].getClassValue());

        }
        return converged;
    }

    /**
     * Recompute the new centers - 2nd version Same as recomputeCenters, but
     * does not check if center stays the same.
     * 
     * @param centers
     *            the input center and output centers
     * @param instOfCentIndexes
     *            the indexes of the instances to the centers
     * @param model
     *            data model information
     */
    private void recomputeCentersFast(Instance[] centers, int[][] instOfCentIndexes) {
        for (int i = 0; i < centers.length; i++) {
            float[] centerValues = centers[i].toArray();
            double val;
            for (int j = 0; j < centerValues.length; j++) {
                val = meanOrMode(data, instOfCentIndexes[i], j);
                centerValues[j] = (float) val;
            }
            centers[i] = new SimpleInstance(centerValues, centers[i].getWeight(), centers[i].isClassSet(), centers[i]
                    .getClassValue());
        }
    }

    /**
     * Computes Mean Or Mode of one attribute on a subset of m_Instances. The
     * subset is defined by an index list.
     * 
     * @param instances
     *            all instances
     * @param instList
     *            the indexes of the instances the mean is computed from
     * @param attIndex
     *            the index of the attribute
     * @return mean value
     */
    private double meanOrMode(Dataset instances, int[] instList, int attIndex) {
        double result, found;
        // int[] counts;
        int numInst = instList.length;

        // if (instances.attribute(attIndex).isNumeric()) {
        result = found = 0;
        for (int j = 0; j < numInst; j++) {
            Instance currInst = instances.getInstance(instList[j]);
            // if (!currInst.isMissing(attIndex)) {
            found += currInst.getWeight();
            result += currInst.getWeight() * currInst.getValue(attIndex);
            // }
        }
        if (MathUtils.eq(found, 0)) {
            return 0;
        } else {
            return result / found;
        }

    }

    /**
     * Assigns instances to centers.
     * 
     * @param tree
     *            KDTree on all instances
     * @param centers
     *            all the input centers
     * @param instOfCent
     *            the instances to each center
     * @param allInstList
     *            list of all instances
     * @param assignments
     *            assignments of instances to centers
     * @param iterationCount
     *            the number of iteration
     * @return true if converged
     * @throws Exception
     *             is something goes wrong
     */
    private boolean assignToCenters(KDTree tree, Instance[] centers, int[][] instOfCent, int[] allInstList,
            int[] assignments, int iterationCount) {

        boolean converged = true;
        if (tree != null) {
            // using KDTree structure for assigning
            converged = assignToCenters(tree, centers, instOfCent, assignments, iterationCount);
            // throw new RuntimeException("Tree not supported");
        } else {
            converged = assignToCenters(centers, instOfCent, allInstList, assignments);
        }
        return converged;
    }

    /**
     * Assign instances to centers using KDtree. First part of conventionell
     * K-Means, returns true if new assignment is the same as the last one.
     * 
     * @param kdtree
     *            KDTree on all instances
     * @param centers
     *            all the input centers
     * @param instOfCent
     *            the instances to each center
     * @param assignments
     *            assignments of instances to centers
     * @param iterationCount
     *            the number of iteration
     * @return true if converged
     * @throws Exception
     *             in case instances are not assigned to cluster
     */
    private boolean assignToCenters(KDTree kdtree, Instance[] centers, int[][] instOfCent, int[] assignments,
            int iterationCount) {

        int numCent = centers.length;
        int numInst = data.size();
        int[] oldAssignments = new int[numInst];

        // WARNING: assignments is "input/output-parameter"
        // should not be null
        if (assignments == null) {
            assignments = new int[numInst];
            for (int i = 0; i < numInst; i++) {
                assignments[0] = -1;
            }
        }

        // WARNING: instOfCent is "input/output-parameter"
        // should not be null
        if (instOfCent == null) {
            instOfCent = new int[numCent][];
        }

        // save old assignments
        for (int i = 0; i < assignments.length; i++) {
            oldAssignments[i] = assignments[i];
        }

        // use tree to get new assignments
        kdtree.centerInstances(centers, assignments, Math.pow(.8, iterationCount));
        boolean converged = true;

        // compare with previous assignment
        for (int i = 0; converged && (i < assignments.length); i++) {
            converged = (oldAssignments[i] == assignments[i]);
            // if (assignments[i] == -1)
            // throw new Exception("Instance " + i + " has not been assigned to
            // cluster.");
        }

        if (!converged) {
            int[] numInstOfCent = new int[numCent];
            for (int i = 0; i < numCent; i++)
                numInstOfCent[i] = 0;

            // count num of assignments per center
            for (int i = 0; i < numInst; i++)
                numInstOfCent[assignments[i]]++;

            // prepare instancelists per center
            for (int i = 0; i < numCent; i++) {
                instOfCent[i] = new int[numInstOfCent[i]];
            }
            // write instance lists per center
            for (int i = 0; i < numCent; i++) {
                int index = -1;
                for (int j = 0; j < numInstOfCent[i]; j++) {
                    index = nextAssignedOne(i, index, assignments);
                    instOfCent[i][j] = index;
                }
            }
        }

        return converged;
    }

    /**
     * Assign instances to centers. Part of conventional K-Means, returns true
     * if new assignment is the same as the last one.
     * 
     * @param centers
     *            all the input centers
     * @param instOfCent
     *            the instances to each center
     * @param allInstList
     *            list of all indexes
     * @param assignments
     *            assignments of instances to centers
     * @return true if converged
     */
    private boolean assignToCenters(Instance[] centers, int[][] instOfCent, int[] allInstList, int[] assignments) {

        // todo: undecided situations
        boolean converged = true; // true if new assignment is the same
        // as the old one
        int[] counts = new int[centers.length];
        int numInst = allInstList.length;
        int numCent = centers.length;
        int[] numInstOfCent = new int[numCent];
        for (int i = 0; i < numCent; i++)
            numInstOfCent[i] = 0;

        // WARNING: assignments is "input/output-parameter"
        // should not be null
        if (assignments == null) {
            assignments = new int[numInst];
            for (int i = 0; i < numInst; i++) {
                assignments[i] = -1;
            }
        }

        // WARNING: instOfCent is "input/output-parameter"
        // should not be null
        if (instOfCent == null) {
            instOfCent = new int[numCent][];
        }

        // set assignments
        for (int i = 0; i < numInst; i++) {
            Instance inst = data.getInstance(allInstList[i]);
            int newC = clusterProcessedInstance(inst, centers);
            counts[newC]++;
            if (converged && newC != assignments[i]) {
                converged = false;
            }

            numInstOfCent[newC]++;
            if (!converged)
                assignments[i] = newC;
        }
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] == 0)
                converged = false;
        }

        // the following is only done
        // if assignments are not the same, because too much effort
        if (!converged) {
            // PFD(0, "assignToCenters -> it has NOT converged");
            for (int i = 0; i < numCent; i++) {
                instOfCent[i] = new int[numInstOfCent[i]];
            }

            for (int i = 0; i < numCent; i++) {
                int index = -1;
                for (int j = 0; j < numInstOfCent[i]; j++) {
                    index = nextAssignedOne(i, index, assignments);
                    instOfCent[i][j] = allInstList[index];
                }
            }
        }
        // else
        // PFD(0, "assignToCenters -> it has converged");

        return converged;
    }

    /**
     * Searches along the assignment array for the next entry of the center in
     * question.
     * 
     * @param cent
     *            index of the center
     * @param lastIndex
     *            index to start searching
     * @param assignments
     *            assignments
     * @return index of the instance the center cent is assigned to
     */
    private int nextAssignedOne(int cent, int lastIndex, int[] assignments) {
        int len = assignments.length;
        int index = lastIndex + 1;
        while (index < len) {
            if (assignments[index] == cent) {
                return (index);
            }
            index++;
        }
        return (-1);
    }

    /**
     * Split centers in their region. Generates random vector of length =
     * variance and adds and substractsx to cluster vector to get two new
     * clusters.
     * 
     * @param rg
     *            random function
     * @param center
     *            the center that is split here
     * @param variance
     *            variance of the cluster
     * @param model
     *            data model valid
     * @return a pair of new centers
     */
    private Instance[] splitCenter(Random rg, Instance center, double variance) {
        m_NumSplits++;
        float[] randomValues = new float[center.size()];
        ArrayOperations.fillRandom(randomValues, rg);
        Instance[] children = new Instance[2];

        // if (m_DebugVectorsFile.exists() && m_DebugVectorsFile.isFile()) {
        // Instance nextVector = getNextDebugVectorsInstance(model);
        // PFD(D_RANDOMVECTOR, "Random Vector from File " + nextVector);
        // r = new AlgVector(nextVector);
        // } else {
        // random vector of length = variance
        // r = new AlgVector(model, random);
        // }
        // System.out.println("PRE: "+Arrays.toString(randomValues));
        ArrayOperations.changeLength(Math.sqrt(variance), randomValues);
        // System.out.println("var="+variance);
        // System.out.println(Arrays.toString(randomValues));
        // System.out.println();
        // PFD(D_RANDOMVECTOR, "random vector *variance " + randomValues);

        // add random vector to center
        float[] centerValues = center.toArray();
        centerValues = ArrayOperations.add(centerValues, randomValues);
        children[0] = new SimpleInstance(centerValues, center.getWeight(), center.isClassSet(), center.getClassValue());
        ;
        // substract random vector to center
        centerValues = center.toArray();
        randomValues = new float[center.size()];
        ArrayOperations.fillRandom(randomValues, rg);
        centerValues = ArrayOperations.substract(centerValues, randomValues);
        children[1] = new SimpleInstance(centerValues, center.getWeight(), center.isClassSet(), center.getClassValue());
        // PFD(0, "second child " + newCenter);

        return children;
    }

    // /**
    // * Split centers in their region. (*Alternative version of splitCenter()*)
    // *
    // * @param random
    // * the random number generator
    // * @param instances
    // * of the region
    // * @param model
    // * @return a pair of new centers
    // */
    // private Instance[] splitCenters(Random random, Dataset instances, Dataset
    // model) {
    // Instance[] children = new Instance[2];// (model, 2);
    // int instIndex = Math.abs(random.nextInt()) % instances.size();
    // children[0] = instances.getInstance(instIndex);
    // int instIndex2 = instIndex;
    // int count = 0;
    // while ((instIndex2 == instIndex) && count < 10) {
    // count++;
    // instIndex2 = Math.abs(random.nextInt()) % instances.size();//
    // numInstances();
    // }
    // children[1] = instances.getInstance(instIndex2);
    //
    // return children;
    // }

    /**
     * Generates new centers randomly. Used for starting centers.
     * 
     * @param random0
     *            random number generator
     * @param model
     *            data model of the instances
     * @param numClusters
     *            number of clusters
     * @return new centers
     */
    private Instance[] makeCentersRandomly(Random random0, Dataset model, int numClusters) {
        Instance[] clusterCenters = new Instance[numClusters];
        m_NumClusters = numClusters;

        // makes the new centers randomly
        for (int i = 0; i < numClusters; i++) {
            int instIndex = Math.abs(random0.nextInt()) % data.size();
            clusterCenters[i] = data.getInstance(instIndex);
        }
        return clusterCenters;
    }

    /**
     * Returns the BIC-value for the given center and instances.
     * 
     * @param instList
     *            The indices of the instances that belong to the center
     * @param center
     *            the center.
     * @param mle
     *            maximum likelihood
     * @param model
     *            the data model
     * @return the BIC value
     */
    private double calculateBIC(int[] instList, Instance center, double mle) {
        int[][] w1 = new int[1][instList.length];
        for (int i = 0; i < instList.length; i++) {
            w1[0][i] = instList[i];
        }
        double[] m = { mle };
        Instance[] w2 = new Instance[1];// (model, 1);
        w2[0] = center;
        return calculateBIC(w1, w2, m);
    }

    /**
     * Calculates the BIC for the given set of centers and instances.
     * 
     * @param instOfCent
     *            The instances that belong to their respective centers
     * @param centers
     *            the centers
     * @param mle
     *            maximum likelihood
     * @return The BIC for the input.
     */
    private double calculateBIC(int[][] instOfCent, Instance[] centers, double[] mle) {
        double loglike = 0.0;
        int numInstTotal = 0;
        int numCenters = centers.length;
        int numDimensions = centers[0].size();// .numAttributes();
        int numParameters = (numCenters - 1) + // probabilities
                numCenters * numDimensions + // means
                numCenters; // variance params
        for (int i = 0; i < centers.length; i++) {
            loglike += logLikelihoodEstimate(instOfCent[i].length, centers[i], mle[i], centers.length * 2);
            numInstTotal += instOfCent[i].length;
        }
        /*
         * diff thats how we did it loglike -= ((centers.numAttributes() + 1.0) *
         * centers.numInstances() * 1) Math.log(count);
         */
        loglike -= numInstTotal * Math.log(numInstTotal);
        // System.out.println ("numInstTotal " + numInstTotal +
        // "calculateBIC res " + loglike);
        loglike -= (numParameters / 2.0) * Math.log(numInstTotal);
        // System.out.println ("numParam " +
        // + numParameters +
        // " calculateBIC res " + loglike);
        return loglike;
    }

    /**
     * Calculates the log-likelihood of the data for the given model, taken at
     * the maximum likelihood point.
     * 
     * @param numInst
     *            number of instances that belong to the center
     * @param center
     *            the center
     * @param distortion
     *            distortion
     * @param numCent
     *            number of centers
     * @return the likelihood estimate
     */
    private double logLikelihoodEstimate(int numInst, Instance center, double distortion, int numCent) {
        // R(n) num of instances of the center -> numInst
        // K num of centers -> not used
        //
        // todo take the diff comments away
        double loglike = 0;
        /* if is new */
        if (numInst > 1) {
            /* diff variance is new */
            //
            // distortion = Sum over instances x of the center(x-center)
            // different to paper; sum should be squared
            //
            // (Sum of distances to center) / R(n) - 1.0
            // different to paper; should be R(n)-K
            double variance = distortion / (numInst - 1.0);

            //
            // -R(n)/2 * log(pi*2)
            //
            double p1 = -(numInst / 2.0) * Math.log(Math.PI * 2.0);
            /*
             * diff thats how we had it double p2 = -((ni *
             * center.numAttributes()) / 2) * distortion;
             */
            //
            // -(R(n)*M)/2 * log(variance)
            //
            double p2 = -(numInst * center.size()) / 2 * Math.log(variance);

            /*
             * diff thats how we had it, the difference is a bug in x-means
             * double p3 = - (numInst - numCent) / 2;
             */
            //
            // -(R(n)-1)/2
            //
            double p3 = -(numInst - 1.0) / 2.0;

            //
            // R(n)*log(R(n))
            //
            double p4 = numInst * Math.log(numInst);

            /*
             * diff x-means doesn't have this part double p5 = - numInst *
             * Math.log(numInstTotal);
             */

            /*
             * loglike = -(ni / 2) * Math.log(Math.PI * 2) - (ni *
             * center.numAttributes()) / 2.0) * logdistortion - (ni - k) / 2.0 +
             * ni * Math.log(ni) - ni * Math.log(r);
             */
            loglike = p1 + p2 + p3 + p4; // diff + p5;
            // the log(r) is something that can be reused.
            // as is the log(2 PI), these could provide extra speed up later on.
            // since distortion is so expensive to compute, I only do that once.
        }
        return loglike;
    }

    /**
     * Calculates the maximum likelihood estimate for the variance.
     * 
     * @param instOfCent
     *            indices of instances to each center
     * @param centers
     *            the centers
     * @return the list of distortions distortion.
     */
    private double[] distortion(int[][] instOfCent, Instance[] centers) {
        double[] distortion = new double[centers.length];
        for (int i = 0; i < centers.length; i++) {
            distortion[i] = 0.0;
            for (int j = 0; j < instOfCent[i].length; j++) {
                distortion[i] += dm.calculateDistance(data.getInstance(instOfCent[i][j]), centers[i]);
            }
        }
        /*
         * diff not done in x-means res *= 1.0 / (count -
         * centers.numInstances());
         */
        return distortion;
    }

    /**
     * Clusters an instance.
     * 
     * @param instance
     *            the instance to assign a cluster to.
     * @param centers
     *            the centers to cluster the instance to.
     * @return a cluster index.
     */
    private int clusterProcessedInstance(Instance instance, Instance[] centers) {

        double minDist = Integer.MAX_VALUE;
        int bestCluster = 0;
        for (int i = 0; i < centers.length; i++) {
            double dist = dm.calculateDistance(instance, centers[i]);

            if (dist < minDist) {
                minDist = dist;
                bestCluster = i;
            }
        }
        ;
        return bestCluster;
    }

    // /**
    // * Clusters an instance that has been through the filters.
    // *
    // * @param instance
    // * the instance to assign a cluster to
    // * @return a cluster number
    // */
    // private int clusterProcessedInstance(Instance instance) throws Exception
    // {
    // double minDist = Integer.MAX_VALUE;
    // int bestCluster = 0;
    // for (int i = 0; i < m_NumClusters; i++) {
    // double dist = dm.calculateDistance(instance, m_ClusterCenters[i]);
    // if (dist < minDist) {
    // minDist = dist;
    // bestCluster = i;
    // }
    // }
    // return bestCluster;
    // }

    // /**
    // * Classifies a given instance.
    // *
    // * @param instance
    // * the instance to be assigned to a cluster
    // * @return the number of the assigned cluster as an integer if the class
    // is
    // * enumerated, otherwise the predicted value
    // * @throws Exception
    // * if instance could not be classified successfully
    // */
    // public int clusterInstance(Instance instance) throws Exception {
    // m_ReplaceMissingFilter.input(instance);
    // Instance inst = m_ReplaceMissingFilter.output();
    //
    // return clusterProcessedInstance(inst);
    // }

    // /**
    // * Returns the number of clusters.
    // *
    // * @return the number of clusters generated for a training dataset.
    // */
    // private int numberOfClusters() {
    // return m_NumClusters;
    // }

    // /**
    // * Returns an enumeration describing the available options.
    // *
    // * @return an enumeration of all the available options
    // */
    // public Enumeration listOptions() {
    // Vector result = new Vector();
    //
    // result
    // .addElement(new Option("\tmaximum number of overall iterations\n" +
    // "\t(default 1).", "I", 1,
    // "-I <num>"));
    //
    // result.addElement(new Option("\tmaximum number of iterations in the
    // kMeans loop in\n"
    // + "\tthe Improve-Parameter part \n" + "\t(default 1000).", "M", 1, "-M
    // <num>"));
    //
    // result.addElement(new Option("\tmaximum number of iterations in the
    // kMeans loop\n"
    // + "\tfor the splitted centroids in the Improve-Structure part \n" +
    // "\t(default 1000).", "J", 1,
    // "-J <num>"));
    //
    // result.addElement(new Option("\tminimum number of clusters\n" +
    // "\t(default 2).", "L", 1, "-L <num>"));
    //
    // result.addElement(new Option("\tmaximum number of clusters\n" +
    // "\t(default 4).", "H", 1, "-H <num>"));
    //
    // result.addElement(new Option("\tdistance value for binary attributes\n" +
    // "\t(default 1.0).", "B", 1,
    // "-B <value>"));
    //
    // result.addElement(new Option("\tUses the KDTree internally\n" +
    // "\t(default no).", "use-kdtree", 0,
    // "-use-kdtree"));
    //
    // result.addElement(new Option("\tFull class name of KDTree class to use,
    // followed\n" + "\tby scheme options.\n"
    // + "\teg: \"weka.core.KDTree -P\"\n" + "\t(default no KDTree class
    // used).", "K", 1,
    // "-K <KDTree class specification>"));
    //
    // result.addElement(new Option("\tcutoff factor, takes the given percentage
    // of the splitted \n"
    // + "\tcentroids if none of the children win\n" + "\t(default 0.0).", "C",
    // 1, "-C <value>"));
    //
    // result.addElement(new Option("\tFull class name of Distance function
    // class to use, followed\n"
    // + "\tby scheme options.\n" + "\t(default weka.core.EuclideanDistance).",
    // "D", 1,
    // "-D <distance function class specification>"));
    //
    // result.addElement(new Option("\tfile to read starting centers from (ARFF
    // format).", "N", 1, "-N <file name>"));
    //
    // result.addElement(new Option("\tfile to write centers to (ARFF format).",
    // "O", 1, "-O <file name>"));
    //
    // result.addElement(new Option("\tThe debug level.\n" + "\t(default 0)",
    // "U", 1, "-U <int>"));
    //
    // result.addElement(new Option("\tThe debug vectors file.", "Y", 1, "-Y
    // <file name>"));
    //
    // Enumeration en = super.listOptions();
    // while (en.hasMoreElements())
    // result.addElement(en.nextElement());
    //
    // return result.elements();
    // }
    //
    // /**
    // * Returns the tip text for this property
    // *
    // * @return tip text for this property
    // */
    // public String minNumClustersTipText() {
    // return "set minimum number of clusters";
    // }
    //
    // /**
    // * Sets the minimum number of clusters to generate.
    // *
    // * @param n
    // * the minimum number of clusters to generate
    // */
    // public void setMinNumClusters(int n) {
    // if (n <= m_MaxNumClusters) {
    // m_MinNumClusters = n;
    // }
    // }
    //
    // /**
    // * Gets the minimum number of clusters to generate.
    // *
    // * @return the minimum number of clusters to generate
    // */
    // public int getMinNumClusters() {
    // return m_MinNumClusters;
    // }
    //
    // /**
    // * Returns the tip text for this property
    // *
    // * @return tip text for this property
    // */
    // public String maxNumClustersTipText() {
    // return "set maximum number of clusters";
    // }
    //
    // /**
    // * Sets the maximum number of clusters to generate.
    // *
    // * @param n
    // * the maximum number of clusters to generate
    // */
    // public void setMaxNumClusters(int n) {
    // if (n >= m_MinNumClusters) {
    // m_MaxNumClusters = n;
    // }
    // }
    //
    // /**
    // * Gets the maximum number of clusters to generate.
    // *
    // * @return the maximum number of clusters to generate
    // */
    // public int getMaxNumClusters() {
    // return m_MaxNumClusters;
    // }
    //
    // /**
    // * Returns the tip text for this property
    // *
    // * @return tip text for this property
    // */
    // public String maxIterationsTipText() {
    // return "the maximum number of iterations to perform";
    // }
    //
    // /**
    // * Sets the maximum number of iterations to perform.
    // *
    // * @param i
    // * the number of iterations
    // * @throws Exception
    // * if i is less than 1
    // */
    // public void setMaxIterations(int i) throws Exception {
    // if (i < 0)
    // throw new Exception("Only positive values for iteration number" + "
    // allowed (Option I).");
    // m_MaxIterations = i;
    // }
    //
    // /**
    // * Gets the maximum number of iterations.
    // *
    // * @return the number of iterations
    // */
    // public int getMaxIterations() {
    // return m_MaxIterations;
    // }
    //
    // /**
    // * Returns the tip text for this property
    // *
    // * @return tip text for this property
    // */
    // public String maxKMeansTipText() {
    // return "the maximum number of iterations to perform in KMeans";
    // }
    //
    // /**
    // * Set the maximum number of iterations to perform in KMeans
    // *
    // * @param i
    // * the number of iterations
    // */
    // public void setMaxKMeans(int i) {
    // m_MaxKMeans = i;
    // m_MaxKMeansForChildren = i;
    // }
    //
    // /**
    // * Returns the tip text for this property
    // *
    // * @return tip text for this property
    // */
    // public String cutOffFactorTipText() {
    // return "the cut-off factor to use";
    // }
    //
    // /**
    // * Sets a new cutoff factor.
    // *
    // * @param i
    // * the new cutoff factor
    // */
    // public void setCutOffFactor(double i) throws Exception {
    // m_CutOffFactor = i;
    // }
    //
    // /**
    // * Gets the cutoff factor.
    // *
    // * @return the cutoff factor
    // */
    // public double getCutOffFactor() {
    // return m_CutOffFactor;
    // }
    //
    // /**
    // * Returns the tip text for this property
    // *
    // * @return tip text for this property suitable for displaying in the
    // * explorer/experimenter gui
    // */
    // public String binValueTipText() {
    // return "Set the value that represents true in the new attributes.";
    // }
    //
    // /**
    // * Gets value that represents true in a new numeric attribute. (False is
    // * always represented by 0.0.)
    // *
    // * @return the value that represents true in a new numeric attribute
    // */
    // public double getBinValue() {
    // return m_BinValue;
    // }
    //
    // /**
    // * Sets the distance value between true and false of binary attributes and
    // * "same" and "different" of nominal attributes
    // *
    // * @param value
    // * the distance
    // */
    // public void setBinValue(double value) {
    // m_BinValue = value;
    // }
    //
    // /**
    // * Returns the tip text for this property
    // *
    // * @return tip text for this property suitable for displaying in the
    // * explorer/experimenter gui
    // */
    // public String distanceFTipText() {
    // return "The distance function to use.";
    // }
    //
    // /**
    // * gets the "binary" distance value
    // *
    // * @param distanceF
    // * the distance function with all options set
    // */
    // public void setDistanceF(DistanceFunction distanceF) {
    // dm = distanceF;
    // }
    //
    // /**
    // * Gets the distance function.
    // *
    // * @return the distance function
    // */
    // public DistanceFunction getDistanceF() {
    // return dm;
    // }
    //
    // /**
    // * Gets the distance function specification string, which contains the
    // class
    // * name of the distance function class and any options to it
    // *
    // * @return the distance function specification string
    // */
    // private String getDistanceFSpec() {
    //
    // DistanceFunction d = getDistanceF();
    // if (d instanceof OptionHandler) {
    // return d.getClass().getName() + " " + Utils.joinOptions(((OptionHandler)
    // d).getOptions());
    // }
    // return d.getClass().getName();
    // }
    //
    // /**
    // * Returns the tip text for this property
    // *
    // * @return tip text for this property suitable for displaying in the
    // * explorer/experimenter gui
    // */
    // public String debugVectorsFileTipText() {
    // return "The file containing the debug vectors (only for debugging!).";
    // }
    //
    // /**
    // * Sets the file that has the random vectors stored. Only used for
    // debugging
    // * reasons.
    // *
    // * @param value
    // * the file to read the random vectors from
    // */
    // public void setDebugVectorsFile(File value) {
    // m_DebugVectorsFile = value;
    // }
    //
    // /**
    // * Gets the file name for a file that has the random vectors stored. Only
    // * used for debugging purposes.
    // *
    // * @return the file to read the vectors from
    // */
    // public File getDebugVectorsFile() {
    // return m_DebugVectorsFile;
    // }
    //
    // /**
    // * Initialises the debug vector input.
    // */
    // public void initDebugVectorsInput() throws Exception {
    // m_DebugVectorsInput = new BufferedReader(new
    // FileReader(m_DebugVectorsFile));
    // m_DebugVectors = new Instances(m_DebugVectorsInput);
    // m_DebugVectorsIndex = 0;
    // }
    //
    // /**
    // * Read an instance from debug vectors file.
    // *
    // * @param model
    // * the data model for the instance
    // */
    // public Instance getNextDebugVectorsInstance(Instances model) throws
    // Exception {
    // if (m_DebugVectorsIndex >= m_DebugVectors.numInstances())
    // throw new Exception("no more prefabricated Vectors");
    // Instance nex = m_DebugVectors.instance(m_DebugVectorsIndex);
    // nex.setDataset(model);
    // m_DebugVectorsIndex++;
    // return nex;
    // }
    //
    // /**
    // * Returns the tip text for this property
    // *
    // * @return tip text for this property suitable for displaying in the
    // * explorer/experimenter gui
    // */
    // public String inputCenterFileTipText() {
    // return "The file to read the list of centers from.";
    // }
    //
    // /**
    // * Sets the file to read the list of centers from.
    // *
    // * @param value
    // * the file to read centers from
    // */
    // public void setInputCenterFile(File value) {
    // m_InputCenterFile = value;
    // }
    //
    // /**
    // * Gets the file to read the list of centers from.
    // *
    // * @return the file to read the centers from
    // */
    // public File getInputCenterFile() {
    // return m_InputCenterFile;
    // }
    //
    // /**
    // * Returns the tip text for this property
    // *
    // * @return tip text for this property suitable for displaying in the
    // * explorer/experimenter gui
    // */
    // public String outputCenterFileTipText() {
    // return "The file to write the list of centers to.";
    // }
    //
    // /**
    // * Sets file to write the list of centers to.
    // *
    // * @param value
    // * file to write centers to
    // */
    // public void setOutputCenterFile(File value) {
    // m_OutputCenterFile = value;
    // }
    //
    // /**
    // * Gets the file to write the list of centers to.
    // *
    // * @return filename of the file to write centers to
    // */
    // public File getOutputCenterFile() {
    // return m_OutputCenterFile;
    // }
    //
    // /**
    // * Returns the tip text for this property
    // *
    // * @return tip text for this property suitable for displaying in the
    // * explorer/experimenter gui
    // */
    // public String KDTreeTipText() {
    // return "The KDTree to use.";
    // }
    //
    // /**
    // * Sets the KDTree class.
    // *
    // * @param k
    // * a KDTree object with all options set
    // */
    // public void setKDTree(KDTree k) {
    // m_KDTree = k;
    // }
    //
    // /**
    // * Gets the KDTree class.
    // *
    // * @return the configured KDTree
    // */
    // public KDTree getKDTree() {
    // return m_KDTree;
    // }
    //
    // /**
    // * Returns the tip text for this property
    // *
    // * @return tip text for this property suitable for displaying in the
    // * explorer/experimenter gui
    // */
    // public String useKDTreeTipText() {
    // return "Whether to use the KDTree.";
    // }
    //
    // /**
    // * Sets whether to use the KDTree or not.
    // *
    // * @param value
    // * if true the KDTree is used
    // */
    // public void setUseKDTree(boolean value) {
    // m_UseKDTree = value;
    // }
    //
    // /**
    // * Gets whether the KDTree is used or not.
    // *
    // * @return true if KDTrees are used
    // */
    // public boolean getUseKDTree() {
    // return m_UseKDTree;
    // }

    // /**
    // * Gets the KDTree specification string, which contains the class name of
    // * the KDTree class and any options to the KDTree
    // *
    // * @return the KDTree string.
    // */
    // private String getKDTreeSpec() {
    //
    // KDTree c = getKDTree();
    // if (c instanceof OptionHandler) {
    // return c.getClass().getName() + " " + Utils.joinOptions(((OptionHandler)
    // c).getOptions());
    // }
    // return c.getClass().getName();
    // }
    //
    // /**
    // * Returns the tip text for this property
    // *
    // * @return tip text for this property suitable for displaying in the
    // * explorer/experimenter gui
    // */
    // public String debugLevelTipText() {
    // return "The debug level to use.";
    // }

    // /**
    // * Sets the debug level. debug level = 0, means no output
    // *
    // * @param d
    // * debuglevel
    // */
    // public void setDebugLevel(int d) {
    // m_DebugLevel = d;
    // }
    //
    // /**
    // * Gets the debug level.
    // *
    // * @return debug level
    // */
    // public int getDebugLevel() {
    // return m_DebugLevel;
    // }

    // /**
    // * Parses a given list of options. <p/>
    // *
    // * <!-- options-start --> Valid options are: <p/>
    // *
    // * <pre> -I &lt;num&gt;
    // * maximum number of overall iterations
    // * (default 1).</pre>
    // *
    // * <pre> -M &lt;num&gt;
    // * maximum number of iterations in the kMeans loop in
    // * the Improve-Parameter part
    // * (default 1000).</pre>
    // *
    // * <pre> -J &lt;num&gt;
    // * maximum number of iterations in the kMeans loop
    // * for the splitted centroids in the Improve-Structure part
    // * (default 1000).</pre>
    // *
    // * <pre> -L &lt;num&gt;
    // * minimum number of clusters
    // * (default 2).</pre>
    // *
    // * <pre> -H &lt;num&gt;
    // * maximum number of clusters
    // * (default 4).</pre>
    // *
    // * <pre> -B &lt;value&gt;
    // * distance value for binary attributes
    // * (default 1.0).</pre>
    // *
    // * <pre> -K &lt;KDTree class specification&gt;
    // * Full class name of KDTree class to use, followed
    // * by scheme options.
    // * eg: "weka.core.KDTree -P"
    // * (default no KDTree class used).</pre>
    // *
    // * <pre> -C &lt;value&gt;
    // * cutoff factor, takes the given percentage of the splitted
    // * centroids if none of the children win
    // * (default 0.0).</pre>
    // *
    // * <pre> -D &lt;distance function class specification&gt;
    // * Full class name of Distance function class to use, followed
    // * by scheme options.
    // * (default weka.core.EuclideanDistance).</pre>
    // *
    // * <pre> -N &lt;file name&gt;
    // * file to read starting centers from (ARFF format).</pre>
    // *
    // * <pre> -O &lt;file name&gt;
    // * file to write centers to (ARFF format).</pre>
    // *
    // * <pre> -U &lt;int&gt;
    // * The debug level.
    // * (default 0)</pre>
    // *
    // * <pre> -Y &lt;file name&gt;
    // * The debug vectors file.</pre>
    // *
    // * <pre> -S &lt;num&gt;
    // * Random number seed.
    // * (default 10)</pre>
    // *
    // * <!-- options-end -->
    // *
    // * @param options
    // * the list of options as an array of strings
    // * @throws Exception
    // * if an option is not supported
    // */
    // public void setOptions(String[] options) throws Exception {
    //
    // String optionString;
    // String funcString;
    //
    // optionString = Utils.getOption('I', options);
    // if (optionString.length() != 0)
    // setMaxIterations(Integer.parseInt(optionString));
    // else
    // setMaxIterations(1);
    //
    // optionString = Utils.getOption('M', options);
    // if (optionString.length() != 0)
    // setMaxKMeans(Integer.parseInt(optionString));
    // else
    // setMaxKMeans(1000);
    //
    // optionString = Utils.getOption('J', options);
    // if (optionString.length() != 0)
    // setMaxKMeansForChildren(Integer.parseInt(optionString));
    // else
    // setMaxKMeansForChildren(1000);
    //
    // optionString = Utils.getOption('L', options);
    // if (optionString.length() != 0)
    // setMinNumClusters(Integer.parseInt(optionString));
    // else
    // setMinNumClusters(2);
    //
    // optionString = Utils.getOption('H', options);
    // if (optionString.length() != 0)
    // setMaxNumClusters(Integer.parseInt(optionString));
    // else
    // setMaxNumClusters(4);
    //
    // optionString = Utils.getOption('B', options);
    // if (optionString.length() != 0)
    // setBinValue(Double.parseDouble(optionString));
    // else
    // setBinValue(1.0);
    //
    // setUseKDTree(Utils.getFlag("use-kdtree", options));
    //
    // if (getUseKDTree()) {
    // funcString = Utils.getOption('K', options);
    // if (funcString.length() != 0) {
    // String[] funcSpec = Utils.splitOptions(funcString);
    // if (funcSpec.length == 0) {
    // throw new Exception("Invalid function specification string");
    // }
    // String funcName = funcSpec[0];
    // funcSpec[0] = "";
    // setKDTree((KDTree) Utils.forName(KDTree.class, funcName, funcSpec));
    // } else {
    // setKDTree(new KDTree());
    // }
    // } else {
    // setKDTree(new KDTree());
    // }
    //
    // optionString = Utils.getOption('C', options);
    // if (optionString.length() != 0)
    // setCutOffFactor(Double.parseDouble(optionString));
    // else
    // setCutOffFactor(0.0);
    //
    // funcString = Utils.getOption('D', options);
    // if (funcString.length() != 0) {
    // String[] funcSpec = Utils.splitOptions(funcString);
    // if (funcSpec.length == 0) {
    // throw new Exception("Invalid function specification string");
    // }
    // String funcName = funcSpec[0];
    // funcSpec[0] = "";
    // setDistanceF((DistanceFunction) Utils.forName(DistanceFunction.class,
    // funcName, funcSpec));
    // } else {
    // setDistanceF(new EuclideanDistance());
    // }
    //
    // optionString = Utils.getOption('N', options);
    // if (optionString.length() != 0) {
    // setInputCenterFile(new File(optionString));
    // m_CenterInput = new BufferedReader(new FileReader(optionString));
    // } else {
    // setInputCenterFile(new File(System.getProperty("user.dir")));
    // m_CenterInput = null;
    // }
    //
    // optionString = Utils.getOption('O', options);
    // if (optionString.length() != 0) {
    // setOutputCenterFile(new File(optionString));
    // m_CenterOutput = new PrintWriter(new FileOutputStream(optionString));
    // } else {
    // setOutputCenterFile(new File(System.getProperty("user.dir")));
    // m_CenterOutput = null;
    // }
    //
    // optionString = Utils.getOption('U', options);
    // int debugLevel = 0;
    // if (optionString.length() != 0) {
    // try {
    // debugLevel = Integer.parseInt(optionString);
    // } catch (NumberFormatException e) {
    // throw new Exception(optionString + "is an illegal value for option -U");
    // }
    // }
    // setDebugLevel(debugLevel);
    //
    // optionString = Utils.getOption('Y', options);
    // if (optionString.length() != 0) {
    // setDebugVectorsFile(new File(optionString));
    // } else {
    // setDebugVectorsFile(new File(System.getProperty("user.dir")));
    // m_DebugVectorsInput = null;
    // m_DebugVectors = null;
    // }
    //
    // super.setOptions(options);
    // }

    // /**
    // * Gets the current settings of SimpleKMeans.
    // *
    // * @return an array of strings suitable for passing to setOptions
    // */
    // public String[] getOptions() {
    // int i;
    // Vector result;
    // String[] options;
    //
    // result = new Vector();
    //
    // result.add("-I");
    // result.add("" + getMaxIterations());
    //
    // result.add("-M");
    // result.add("" + getMaxKMeans());
    //
    // result.add("-J");
    // result.add("" + getMaxKMeansForChildren());
    //
    // result.add("-L");
    // result.add("" + getMinNumClusters());
    //
    // result.add("-H");
    // result.add("" + getMaxNumClusters());
    //
    // result.add("-B");
    // result.add("" + getBinValue());
    //
    // if (getKDTree() != null) {
    // result.add("-K");
    // result.add("" + getKDTreeSpec());
    // }
    //
    // result.add("-C");
    // result.add("" + getCutOffFactor());
    //
    // if (getDistanceF() != null) {
    // result.add("-D");
    // result.add("" + getDistanceFSpec());
    // }
    //
    // if (getInputCenterFile().exists() && getInputCenterFile().isFile()) {
    // result.add("-N");
    // result.add("" + getInputCenterFile());
    // }
    //
    // if (getOutputCenterFile().exists() && getOutputCenterFile().isFile()) {
    // result.add("-O");
    // result.add("" + getOutputCenterFile());
    // }
    //
    // int dL = getDebugLevel();
    // if (dL > 0) {
    // result.add("-U");
    // result.add("" + getDebugLevel());
    // }
    //
    // if (getDebugVectorsFile().exists() && getDebugVectorsFile().isFile()) {
    // result.add("-Y");
    // result.add("" + getDebugVectorsFile());
    // }
    //
    // options = super.getOptions();
    // for (i = 0; i < options.length; i++)
    // result.add(options[i]);
    //
    // return (String[]) result.toArray(new String[result.size()]);
    // }

    /**
     * Return a string describing this clusterer.
     * 
     * @return a description of the clusterer as a string
     */
    public String toString() {
        StringBuffer temp = new StringBuffer();

        temp.append("\nXMeans\n======\n");

        temp.append("Requested iterations            : " + m_MaxIterations + "\n");
        temp.append("Iterations performed            : " + m_IterationCount + "\n");
        temp.append("kMeans did not converge\n");
        temp.append("  but was stopped by max-loops " + m_KMeansStopped + " times (max kMeans-iter) = \n\n");
        temp.append("Splits prepared                 : " + m_NumSplits + "\n");
        temp.append("Splits performed                : " + m_NumSplitsDone + "\n");
        temp.append("Cutoff factor                   : " + m_CutOffFactor + "\n");
        double perc;
        if (m_NumSplitsDone > 0)
            perc = (((double) m_NumSplitsStillDone) / ((double) m_NumSplitsDone)) * 100.0;
        else
            perc = 0.0;
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        temp.append("Percentage of splits accepted \n" + "by cutoff factor                : " + nf.format(perc)
                + " %\n");
        temp.append("------\n");

        temp.append("Cutoff factor                   : " + m_CutOffFactor + "\n");
        temp.append("------\n");
        temp.append("\nCluster centers                 : " + m_NumClusters + " centers\n");
        for (int i = 0; i < m_NumClusters; i++) {
            temp.append("\nCluster " + i + "\n           ");
            for (int j = 0; j < m_ClusterCenters[0].size(); j++) {
                // if (m_ClusterCenters.attribute(j).isNominal()) {
                // temp.append(" " + m_ClusterCenters.attribute(j).value((int)
                // m_ClusterCenters.instance(i).value(j)));
                // } else {
                temp.append(" " + m_ClusterCenters[i].getValue(j));
                // }
            }
        }
        nf.setMaximumFractionDigits(6);
        if (m_Mle != null)
            temp.append("\n\nDistortion: " + nf.format(ArrayOperations.sum(m_Mle)) + "\n");
        temp.append("BIC-Value : " + nf.format(bic) + "\n");
        return temp.toString();
    }

//    /**
//     * Print centers for debug.
//     * 
//     * @param debugLevel
//     *            level that gives according messages
//     */
//    private void PrCentersFD(int debugLevel) {
//
//        for (int i = 0; i < m_ClusterCenters.length; i++) {
//            System.out.println(m_ClusterCenters[i]);
//
//        }
//    }

    // /**
    // * Tests on debug status.
    // *
    // * @param debugLevel
    // * level that gives according messages
    // * @return true if debug level is set
    // */
    // private boolean TFD(int debugLevel) {
    // return (debugLevel == m_DebugLevel);
    // }

//    /**
//     * Does debug printouts.
//     * 
//     * @param debugLevel
//     *            level that gives according messages
//     * @param output
//     *            string that is printed
//     */
//    private void PFD(int debugLevel, String output) {
//
//        System.out.println(output);
//    }
    //
    // /**
    // * Does debug printouts.
    // *
    // * @param output
    // * string that is printed
    // */
    // private void PFD_CURR(String output) {
    // if (m_CurrDebugFlag)
    // System.out.println(output);
    // }

    // /**
    // * Main method for testing this class.
    // *
    // * @param argv
    // * should contain options
    // */
    // public static void main(String[] argv) {
    // runClusterer(new XMeans(), argv);
    // }
}