/**
 * TestSymmetricalUncertainty.java
 *
 * %SVN.HEADER%
 */
package junit.filter;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.core.SimpleDataset;
import net.sf.javaml.core.SimpleInstance;
import net.sf.javaml.filter.attribute.eval.SymmetricalUncertaintyAttributeEvaluation;

import org.junit.Test;

public class TestSymmetricalUncertainty {
    
    @Test
    public void testSU() {
        // double[] vals = { 1, 2, 3, 4, 5, 6 };

        Dataset data = new SimpleDataset();
        for (int i = 0; i < 10; i++) {
            double[] vals = { vary(5 * (i / 2), 1), vary(1, 0) };
            Instance inst = new SimpleInstance(vals, i / 2);

            data.add(inst);

        }
        System.out.println(data);
        SymmetricalUncertaintyAttributeEvaluation ga = new SymmetricalUncertaintyAttributeEvaluation();
        ga.build(data);
        System.out.println(ga.evaluateAttribute(0));
        System.out.println(ga.evaluateAttribute(1));
       

    }

    private double vary(double i, double j) {
        return i + j * Math.random();
    }
}
