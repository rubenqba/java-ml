/**
 * MultiKMeans.java
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
 * Copyright (c) 2006-2007, Andreas De Rijcke
 * 
 * Project: http://sourceforge.net/projects/java-ml/
 * 
 */
package net.sf.javaml.clustering;

import net.sf.javaml.clustering.evaluation.ClusterEvaluation;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.distance.DistanceMeasure;

/**
 * This class implements an extension of SimpleKMeans (SKM). SKM will be run
 * several iterations with the same k value. Each clustering result is evaluated
 * with an evaluation score, the result with the best score will be returned as
 * final result.
 * 
 * @param clusters
 *            the number of clusters
 * @param iterations
 *            the number of iterations in SKM
 * @param repeats
 *            the number of SKM repeats
 * @param dm
 *            distance measure used for internal cluster evaluation
 * @param ce
 *            clusterevaluation methode used for internal cluster evaluation
 * 
 * @author Thomas Abeel
 * @author Andreas De Rijcke
 * 
 */

public class MultiKMeans implements Clusterer {
	private int repeats, clusters, iterations;

	private DistanceMeasure dm;

	private ClusterEvaluation ce;

	public MultiKMeans(int clusters, int iterations, int repeats,
			DistanceMeasure dm, ClusterEvaluation ce) {
		this.clusters = clusters;
		this.iterations = iterations;
		this.repeats = repeats;
		this.dm = dm;
		this.ce = ce;

	}

	public Dataset[] executeClustering(Dataset data) {
		SimpleKMeans km = new SimpleKMeans(this.clusters, this.iterations,
				this.dm);
		Dataset[] bestClusters = km.executeClustering(data);
		double bestScore = this.ce.score(bestClusters);
		for (int i = 0; i < repeats; i++) {
			Dataset[] tmpClusters = km.executeClustering(data);
			double tmpScore = this.ce.score(tmpClusters);
			if (this.ce.compareScore(bestScore, tmpScore)) {
				bestScore = tmpScore;
				bestClusters = tmpClusters;
			}
		}
		return bestClusters;
	}
}
