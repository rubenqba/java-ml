/**
 * %SVN.HEADER% 
 */
package net.sf.javaml.classification.meta;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.sf.javaml.classification.AbstractClassifier;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.DatasetTools;

/**
 * Bootstrap aggregating (Bagging) meta learner. This is the most basic
 * implementation of Bagging.
 * 
 * {@jmlSource}
 * 
 * @author Thomas Abeel
 * 
 */
public class SimpleBagging extends AbstractClassifier {

    private static final long serialVersionUID = 208101377048893813L;

    private Classifier[] classifiers;

    private Random rg;

    public SimpleBagging(Classifier[] classifiers){
        this(classifiers,new Random(System.currentTimeMillis()));
    }
    
    public SimpleBagging(Classifier[] classifiers, Random rg) {
        this.classifiers = classifiers;
        this.rg = rg;
    }

    /* Reference to the training data */
    private Dataset reference = null;

    @Override
    public void buildClassifier(Dataset data) {
        this.reference = data;
        for (int i = 0; i < classifiers.length; i++) {
            Dataset sample = DatasetTools.bootstrap(data, data.size(), rg);
            classifiers[i].buildClassifier(sample);
        }
    }

    @Override
    public Map<Object, Double> classDistribution(Instance instance) {
        Map<Object, Double> membership = new HashMap<Object, Double>();
        for (Object o : reference.classes())
            membership.put(o, 0.0);
        for (int i = 0; i < classifiers.length; i++) {
            Object prediction = classifiers[i].classify(instance);
            membership.put(prediction, membership.get(prediction) + (1.0 / classifiers.length));
        }

        return membership;

    }
}
