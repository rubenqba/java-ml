/**
 * DimensionSpeedTestDatasets.java
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
package net.sf.javaml.data.clustering;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.core.SimpleDataset;
import net.sf.javaml.core.SimpleInstance;

/**
 * This class can generate datasets with variable dimension to test the time
 * complexity and performance of clustering algorithms on datasets with
 * different dimension.
 * 
 * All datasets contain 4 clusters with 1000 instances each.
 * 
 * All datasets are normalized with the values in each dimension between 0 and
 * 1.
 * 
 * All instances have no class set.
 * 
 * The values in each dimension within a cluster are Gaussian distributed with a
 * standard deviation of 0.1.
 * 
 * @author Thomas Abeel
 * 
 */
public class VariableDimensionDatasets {
    /**
     * Call all dataset generation algorithms and write the datasets to files.
     * 
     * @param args
     */
    public static void main(String[] args) {
         for (int i = 2; i < 2000; i*=2) {
            write(createNd(i), "dim" + i + ".data");
        }

    }

    private static Dataset createNd(int n) {
        Dataset out = new SimpleDataset();
        float small = 1.0f / 4.0f;
        float large = 3.0f / 4.0f;
        float clusterSpread = 0.1f;
        Random rg = new Random(System.currentTimeMillis());
        for (int i = 0; i < 1000; i++) {

            // lower left
            float[] vec1 = new float[n];
            vec1[0] = (float) ((rg.nextGaussian() * clusterSpread) + small);
            vec1[1] = (float) ((rg.nextGaussian() * clusterSpread) + small);
            out.addInstance(new SimpleInstance(vec1));

            // // upper left
            float[] vec2 = new float[n];
            vec2[0] = (float) ((rg.nextGaussian() * clusterSpread) + small);
            vec2[1] = (float) ((rg.nextGaussian() * clusterSpread) + large);
            out.addInstance(new SimpleInstance(vec2));
            // // lower righ
            float[] vec3 = new float[n];
            vec3[0] = (float) ((rg.nextGaussian() * clusterSpread) + large);
            vec3[1] = (float) ((rg.nextGaussian() * clusterSpread) + small);
            out.addInstance(new SimpleInstance(vec3));
            // // upper right
            float[] vec4 = new float[n];
            vec4[0] = (float) ((rg.nextGaussian() * clusterSpread) + large);
            vec4[1] = (float) ((rg.nextGaussian() * clusterSpread) + large);
            out.addInstance(new SimpleInstance(vec4));
        }
        return out;
    }

    private static void write(Dataset data, String fileName) {
        try {
            PrintWriter out = new PrintWriter(fileName);
            for (int i = 0; i < data.size(); i++) {
                Instance tmp = data.getInstance(i);
                out.print(tmp.getValue(0));
                for (int j = 1; j < tmp.size(); j++)
                    out.print("\t" + tmp.getValue(j));
                out.println();
            }
            out.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    
}
