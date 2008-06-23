/**
 * NormalizedEuclideanSimilarity.java
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
package net.sf.javaml.distance;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;

/**
 * XXX DOC
 */
public class NormalizedEuclideanSimilarity extends AbstractSimilarity {
    /**
     * 
     */
    private static final long serialVersionUID = 3840902505037993972L;
    /**
     * XXX DOC
     */
    private NormalizedEuclideanDistance dm;

    /**
     * XXX DOC
     */
    public NormalizedEuclideanSimilarity(Dataset data) {
        super();
        this.dm = new NormalizedEuclideanDistance(data);
    }

    /**
     * XXX DOC
     */
    public double measure(Instance i, Instance j) {
        return 1 - dm.calculateDistance(i, j);
    }

 

}
