/**
 * AdaptiveQualityBasedClustering.java, 9-feb-2007
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
 * Copyright (c) 2007, Andreas De Rijcke
 * 
 * Project: http://sourceforge.net/projects/java-ml/
 * 
 */

package net.sf.javaml.clustering;

import java.util.Vector;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.core.SimpleDataset;
import net.sf.javaml.core.SimpleInstance;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.distance.EuclideanDistance;
import net.sf.javaml.filter.NormalizeMean;
import net.sf.javaml.optimization.ExpectationMaximization;

/**
 * 
 * This class implements an Adaptive Quality-based Clustering Algorithm, based
 * on the implementation in MATLAB by De Smet et al., ESAT - SCD (SISTA),
 * K.U.Leuven, Belgium.
 * 
 * @author Andreas De Rijcke
 * 
 */

public class AdaptiveQualityBasedClustering implements Clusterer {

	// user defined parameters
	private int minInstances = 2;

	private double significanceLevel = 0.95;

	private int maxIterMain = 50;

	// internal tuning parameters

	private int maxIter = 50;

	private double div = 0.03333333333333;

	// 1 / 30;

	private double accurRad = 0.1;

	// other variables
	private int instanceLength;

	private double dimension;

	private double rk_prelim;

	private double rk;

	private double rad;

	private double deltarad;

	private double variance;

	private Instance ck;

	private Instance newMean;

	private Vector<Vector<Instance>> finalClusters = new Vector<Vector<Instance>>();

	private DistanceMeasure dm = new EuclideanDistance();

	private NormalizeMean normMean = new NormalizeMean();

	private ExpectationMaximization em = new ExpectationMaximization();

	// calculates mean instance of given dataset/cluster
	public Instance mean(Vector<Instance> data, int instanceLength) {
		Instance in;
		float[] sumVector = new float[instanceLength];
		for (int i = 0; i < data.size(); i++) {
			in = data.get(i);
			for (int j = 0; j < instanceLength; j++) {
				sumVector[j] += in.getValue(j);
			}
		}
		for (int j = 0; j < instanceLength; j++) {
			sumVector[j] /= data.size();
		}
		Instance mean = new SimpleInstance(sumVector);
		return mean;
	}

	// calculate max distance between cluster center ck and instances in the
	// cluster
	public double maxDist(Vector<Instance> data, Instance mean) {
		double maxDist = Double.MIN_VALUE;
		for (int i = 0; i < data.size(); i++) {
			Instance x = data.get(i);
			double distance = dm.calculateDistance(x, mean);
			if (maxDist < distance) {
				maxDist = distance;
			}
		}
		return maxDist;
	}

	// calculate instances in sphere with ck as center en rad as radius
	public Vector<Instance> newCluster(Vector<Instance> data, Instance mean,
			double rad) {
		Vector<Instance> newCluster = new Vector<Instance>();
		for (int i = 0; i < data.size(); i++) {
			Instance x = data.get(i);
			double distance = dm.calculateDistance(x, mean);
			if (distance < rad) {
				newCluster.add(x);
			}
		}
		System.out.println("old cluster size " + data.size()
				+ ", new cluster size " + newCluster.size());
		return newCluster;
	}

	// main
	public Dataset[] executeClustering(Dataset data) {
		instanceLength = data.getInstance(0).size();
		// stop criterion
		double initDatasizeStopCrit = data.size() * 0.2;
		// normalize dataset
		Dataset dataNorm = normMean.filterDataset(data);
		// convert dataset of instances to vector of instances
		Vector<Instance> all = new Vector<Instance>();
		// temporarily processing vector
		Vector<Instance> cluster = new Vector<Instance>();
		for (int i = 0; i < dataNorm.size(); i++) {
			Instance in = dataNorm.getInstance(i);
			all.add(in);
			cluster.add(in);
		}

		// initiation

		// calculate preliminary estimate of radius
		dimension = dataNorm.getInstance(0).size();
		rk_prelim = Math.sqrt((dimension - 1) / 2);
		System.out.println("initiation: dimension " + dimension
				+ ", rk_prelim " + rk_prelim);

		// initiate clustercenter, radius and calculated deltarad
		ck = mean(cluster, instanceLength);
		rad = maxDist(cluster, ck);
		deltarad = (rad - rk_prelim) * div;
		System.out.println("initiation: initial rad " + rad + ", deltarad "
				+ deltarad);
		rad = rad - deltarad;
		System.out.println("step 1: new rad " + rad);

		int iterator = 1, endSign = 1, nonvalidcluster = 0;
		while (iterator < maxIterMain && endSign != 0) {
			// while (iterator < maxIterMain) {
			System.out.println("iterator main " + iterator);

			// step1: locate cluster center
			System.out.println("step 1: new rad " + rad);
			cluster = newCluster(cluster, ck, rad);
			if (cluster.size() <= minInstances) {
				nonvalidcluster = 1;
				System.out.println("step 1: non valid cluster found ");
			} else {
				nonvalidcluster = 0;
			}
			if (nonvalidcluster == 0) {
				newMean = mean(cluster, instanceLength);
				rad = maxDist(cluster, newMean);
				// move cluster center to new mean
				ck = newMean;
				int iter = 0, stop = 0;
				// while ((iter < maxIter && newMean != ck) || rad > rk_prelim)
				// {
				while ((iter < maxIter && stop < 40) || rad > rk_prelim ) {
					iter++;
					System.out.println("step 1: iter" + iter);
					if (rad > rk_prelim) {
						rad = rad - deltarad;
						System.out.println("step 1: new rad " + rad);
					}
					cluster = newCluster(cluster, ck, rad);
					newMean = mean(cluster, instanceLength);
					double distCkNewMean = dm.calculateDistance(ck, newMean);
					if (distCkNewMean == 0) {
						System.out.println("convergence.");
						// cluster = newCluster(cluster, ck, rad);
						stop++;
						System.out.println("step 1: stop " + stop);
					} else {
						System.out.println("ck still not equal to new mean.");
						// cluster = newCluster(cluster, ck, rad);
						stop = 0;
					}
					ck = newMean;
				}
			}

			// step 2:recalculate radius: calculation of sigma and a prior prob
			// pc and pb via EM

			// temporarily vector for variance calculation via EM
			Vector<Double> varianceEst = new Vector<Double>();
			double pc = em.em(all, cluster, ck, rk_prelim, dimension,
					varianceEst);
			double pb = 1 - pc;
			variance = varianceEst.get(0);
			System.out.println("step 2 : new var " + variance + " new pc " + pc
					+ " new pb " + pb);
			if (pc == 0 & varianceEst == null) {
				System.out.println("EM algorithm did not converge.");
				return null;
			}
			// calculation new radius
			double dimD = dimension - 2;
			double sD = em.sD(dimD);
			double sD1 = em.sD(dimD + 1);
			System.out.println("step 2 : sD " + sD + " sD1 " + sD1);
			double c1 = sD
					/ Math.pow((2 * Math.PI * variance * variance), dimD / 2);
			double c2 = sD / (sD1 * Math.pow(dimD + 1, dimD / 2));
			double c3 = Math.abs(1 - (1 / significanceLevel));
			System.out.println("step 2 : c1 " + c1 + " c2 " + c2 + " c3 " + c3);
			double tmp = Math.log(pb * c2 / (pc * c1 * c3));
			double tmp2 = 2 * variance * variance * tmp;
			System.out.println("step 2 : tmp " + tmp + " tmp2 " + tmp2);
			if (tmp2 < 0) {
				System.out.println("step 2 : tmp2 kleiner dan 0");
				return null;
			}
			rk = Math.sqrt(tmp2);
			System.out.println("step 2 : new rk " + rk);
			double tmp3 = Math.abs((rk - rk_prelim) / rk_prelim);
			System.out.println("step 2 : verschil rk, rk_prelim " + tmp3+" accurRad "+accurRad);
			System.out.println("end step 2");

			if (Math.abs((rk - rk_prelim) / rk_prelim) < accurRad) {
				cluster = newCluster(cluster, ck, rk);
				if (cluster.size() > minInstances) {
					Vector<Instance> finalCluster = new Vector<Instance>();
					finalCluster.addAll(cluster);
					finalClusters.add(finalCluster);
					System.out
							.println("---- valid cluster added to final clusters, size "
									+ cluster.size() + " ----");
					System.out.println("---- final clusters "
							+ finalClusters.size() + " ----");
					all.removeAll(cluster);
					cluster.clear();
					cluster.addAll(all);
					System.out.println("---- new cluster size "
							+ cluster.size() + " ----");
					if (cluster.size() <= initDatasizeStopCrit) {
						endSign = 0;
						System.out.println("---- endSign put to 0 ----");
					}
					newMean = mean(cluster, instanceLength);
					ck = newMean;
					rad = maxDist(cluster, ck);
					System.out.println("---- reset rad to maxdist " + rad
							+ " ----");
				} else {
					System.out
							.println("-- non valid cluster: reload cluster with rest data: new cluster size "
									+ cluster.size() + " --");
					cluster.clear();
					cluster.addAll(all);
					newMean = mean(cluster, instanceLength);
					ck = newMean;
					rad = maxDist(cluster, ck);
					System.out
							.println("-- reset rad to maxdist " + rad + " --");
				}
			}
			
			// update preliminary radius estimate with new estimate
			rk_prelim = rk;
			System.out.println("-- update rk_prelim met rk --");
			iterator++;
		}

		// for test purpose
		System.out.println("final number of clusters " + finalClusters.size());
		for (int i = 0; i < finalClusters.size(); i++) {
			System.out.println("cluster " + i + " size "
					+ finalClusters.get(i).size());
		}
		// write results to output
		System.out.println("write clusters to output");
		Dataset[] output = new Dataset[finalClusters.size()];
		for (int i = 0; i < finalClusters.size(); i++) {
            output[i]=new SimpleDataset();
			Vector<Instance> getCluster = new Vector<Instance>();
			getCluster = finalClusters.get(i);
			System.out.println("cluster " + i + " size " + getCluster.size());
			for (int j = 0; j < getCluster.size(); j++) {
				System.out.println("cluster " + i + " instance "+j+" " + getCluster.get(j));
				output[i].addInstance(normMean.unfilterInstance(getCluster.get(j)));
			}
		}
		System.out.println("output written");
		return output;
	}
}
