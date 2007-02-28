/**
 * MCL.java, 22-nov-2006
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
 * Copyright (c) 2006, Thomas Abeel, Andreas De Rijcke
 * 
 * Project: http://sourceforge.net/projects/java-ml/
 * 
 */
package net.sf.javaml.clustering.mcl;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Vector;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.core.SimpleDataset;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.distance.NormalizedEuclideanDistance;

public class MCL implements Clusterer {

    public MCL(DistanceMeasure dm){
        this.dm=dm;
    }
    
	private DistanceMeasure dm;

	// Maximum difference between row elements and row square sum (measure of
	// idempotence)
	double maxResidual = 0.001;

	// inflation exponent for Gamma operator
	double pGamma = 2.0;

	// loopGain values for cycles
	double loopGain = 0.;

	// maximum value considered zero for pruning operations
	double maxZero = 0.001;

	public Dataset[] executeClustering(Dataset data) {
			// convert dataset to matrix of distances
			double[][] dataConverted = new double[data.size()][data.size()];
			double sum=0;
			for (int i = 0; i < data.size(); i++) {
				for (int j = 0; j < data.size(); j++) {
					double distance = dm.calculateDistance(data.getInstance(i),
							data.getInstance(j));
					if (i == j) {
						dataConverted[i][i] = distance;
					} else {
						dataConverted[i][j] = distance;
						dataConverted[j][i] = distance;
					}
                    sum+=distance/(data.size()*data.size());
				}
                
			}
            System.out.println("Cut-off distance="+sum);
            sum=0.85;
            for (int i = 0; i < data.size(); i++) {
                for (int j = 0; j < data.size(); j++) {
                    if(dataConverted[i][j]<sum)
                        dataConverted[i][j]=0;
                }
            }
			//System.out.println("dataset converted");
			SparseMatrix dataSparseMatrix = new SparseMatrix(dataConverted);
			MarkovClustering mcl = new MarkovClustering();
			SparseMatrix matrix = mcl.run(dataSparseMatrix, maxResidual,
					pGamma, loopGain, maxZero);
			System.out.println("mcl run finished");

			// convert matrix to output dataset:
			int[] sparseMatrixSize = matrix.getSize();
			//System.out.println("sparseMatrixSize: " + sparseMatrixSize[0]);
			
			// find number of attractors (non zero values) in diagonal
			int attractors = 0;
			for (int i = 0; i < sparseMatrixSize[0]; i++) {
				double val = matrix.get(i, i);
				if (val != 0) {
					attractors++;
				}
			}
			System.out.println("# attractors: " + attractors);
            System.out.print("final cluster size: ");
			// create cluster for each attractor with value close to 1
			Vector<Vector<Instance>> finalClusters = new Vector<Vector<Instance>>();
			
			for (int i = 0; i < sparseMatrixSize[0]; i++) {
				Vector<Instance> cluster = new Vector<Instance>();
				double val = matrix.get(i, i);
				if (val >= 0.98 ) {
					//System.out.println("valid attractor found");
					for (int j = 0 ; j < sparseMatrixSize[0]; j++){
						double value = matrix.get(j, i);
						if ( value != 0){
							cluster.add(data.getInstance(j));	
							//System.out.println("instance added");
						}
					}
					System.out.print(cluster.size()+", ");
					finalClusters.add(cluster);
				}
			}
            System.out.println();
			//System.out.println("finalClusterssize: "+ finalClusters.size());
			
			//System.out.println("start data adding to dataset");
			Dataset[] output = new Dataset[finalClusters.size()];
			for (int i = 0; i < finalClusters.size(); i++) {
				output[i]=new SimpleDataset();
			}
			for (int i = 0; i < finalClusters.size(); i++) {
				Vector<Instance> getCluster = new Vector<Instance>();
				getCluster = finalClusters.get(i);
				for (int j = 0; j < getCluster.size(); j++) {
					output[i].addInstance(getCluster.get(j));
				}
			}
			
			return output;
		
	}
}
