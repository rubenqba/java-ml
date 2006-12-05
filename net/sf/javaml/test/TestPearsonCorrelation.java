/**
 * TestPearsonCorrelation.java, 4-dec-2006
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
package net.sf.javaml.test;

import net.sf.javaml.core.Instance;
import net.sf.javaml.core.SimpleInstance;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.distance.PearsonCorrelationCoefficient;

public class TestPearsonCorrelation {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Instance x=new SimpleInstance(new float[]{1,2,3});
        Instance y=new SimpleInstance(new float[]{2,5,6});
        DistanceMeasure dm=new PearsonCorrelationCoefficient();
        System.out.println("PCC= "+dm.calculateDistance(x,y));

    }

}