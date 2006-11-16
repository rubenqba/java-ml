/**
 * BICScore.java, 31-okt-06
 *
 * This file is part of the Java Machine Learning API
 * 
 * php-agenda is free software; you can redistribute it and/or modify
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
 * Copyright (c) 2006, Andreas De Rijcke
 * 
 * Project: http://sourceforge.net/projects/java-ml/
 * 
 */


package net.sf.javaml.clustering.evaluation;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.distance.DistanceMeasureFactory;
/**
 * Check code
 * @author Thomas Abeel
 * 
 */
@Deprecated
public class BICScore {
	
	public static double varianceEstimate (Dataset data,Instance centroid, int numberOfClusters){
		double variance;
		double s = data.size();
		double k = numberOfClusters;
		double sum = 0;
		DistanceMeasure dm=DistanceMeasureFactory.getEuclideanDistanceMeasure();
		for (int i = 0; i < s; i++) {
			sum += dm.calculateDistance(data.getInstance(i), centroid);
		}
		variance = (1/(s-k))*sum;
		return variance;
	}
	
	
	public static double logLikeliHood (Dataset data, double variance, int numberOfClusters, int initialDataSetSize, double dataDimension){
		double loglike;
		double c = data.size();
		double v = variance;
		double k = numberOfClusters;
		double r = initialDataSetSize;
		double d = dataDimension;
		loglike = (-c/2)*(Math.log(2*Math.PI)) - ((c*d)/2)*(Math.log(v)) + c*(Math.log(c)) - c*(Math.log(r))-(c-k)/2;
		return loglike;
	}
	
	
	public static double bicScore (Dataset data, double loglike, double variance, int numberOfClusters, double dataDimension){
		double bic;
		double r = data.size();
		double l = loglike;
		double v = variance;
		double k = numberOfClusters;
		double d = dataDimension;
		double p = (k-1)+d*k+v;
		System.out.println("p = "+p);
		bic = l - (p/2)*Math.log(r);
		return bic;
	}

}