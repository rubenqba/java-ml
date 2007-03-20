/** VisualTestMultiKMeans.java, 25-okt-2006
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
package net.sf.javaml.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.javaml.clustering.MultiKMeans;
import net.sf.javaml.clustering.evaluation.Tau;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.EuclideanDistance;
import net.sf.javaml.tools.DatasetGenerator;

public class VisualTestMultiKMeans extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 5608683822417234316L;

    /**
     * @param args
     */
    public static void main(String[] args) {
        JFrame window = new JFrame("Multi K-means test");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setContentPane(new VisualTestMultiKMeans());
        window.pack();
        window.setVisible(true);

    }

    public VisualTestMultiKMeans() {
        this.setLayout(new GridLayout(0, 3));
        int space = 300;
        Dataset data = DatasetGenerator.createClusterSquareDataset(space, 10);

        this.add(createLabel(data, Color.BLACK, space, space, null, null, 0));
        MultiKMeans km = new MultiKMeans(4, 20, 10 , new EuclideanDistance(), new Tau(new EuclideanDistance()));
        Dataset[]clusters=km.executeClustering(data);

        
        Color[] colors = { Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA };
        for (int i = 0; i < 4; i++) {
            this.add(createLabel(clusters[i], colors[i], space, space, km, colors, i));
        }

    }

    private JLabel createLabel(Dataset data, Color color, int width, int height, MultiKMeans km, Color[] colors, int i) {
        return new ClusterLabel(data, color, width, height, km, colors, i);
    }

    class ClusterLabel extends JLabel {

        public void setData(Dataset x) {
            this.data = x;
        }

        private static final long serialVersionUID = -5642750374875570050L;

        private Dataset data;

        private Color color;

     
        public ClusterLabel(Dataset data, Color color, int width, int height, MultiKMeans km2, Color[] colors, int i) {
            this.setPreferredSize(new Dimension(width, height));
            this.data = data;
            this.color = color;
           this.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(color);
            for (int i = 0; i < data.size(); i++) {
                Instance in = data.getInstance(i);
                g.fillOval((int) in.getValue(0) - 1, (int) in.getValue(1) - 1, 2, 2);

            }
            

        }

    }
}
