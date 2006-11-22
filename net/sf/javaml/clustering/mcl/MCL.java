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
 * Copyright (c) 2006, Thomas Abeel
 * 
 * Project: http://sourceforge.net/projects/java-ml/
 * 
 */
package net.sf.javaml.clustering.mcl;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;

// TODO implements bridge between Gregory's implementation and the interfaces.
public class MCL implements Clusterer {

    public void buildClusterer(Dataset data) {
        // TODO Auto-generated method stub

    }

    public int predictCluster(Instance instance) {
        // TODO Auto-generated method stub
        return 0;
    }

    public double[] predictMembershipDistribution(Instance instance) {
        // TODO Auto-generated method stub
        return null;
    }

    public int getNumberOfClusters() {
        // TODO Auto-generated method stub
        return 0;
    }

}
