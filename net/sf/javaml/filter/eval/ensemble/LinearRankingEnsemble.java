/**
 * LinearEnsemble.java
 *
 * %SVN.HEADER%
 */
package net.sf.javaml.filter.eval.ensemble;

import java.util.Random;


import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DatasetTools;
import net.sf.javaml.filter.eval.IAttributeRanking;
import net.sf.javaml.utils.ArrayUtils;

public class LinearRankingEnsemble implements IAttributeRanking {

    private IAttributeRanking[] aes;

    private Random rg;
    public LinearRankingEnsemble(IAttributeRanking[] aes,Random rg) {
        this.aes = aes;
    }

    private int[] ranking;

    public void build(Dataset data) {
        int numAtt = data.noAttributes();
        double[] sum = new double[numAtt];
        for (IAttributeRanking ae : aes) {
            ae.build(DatasetTools.bootstrap(data, (int) (data.size() * 0.9 + 1),rg));
            for (int i = 0; i < numAtt; i++)
                sum[i] += ae.getRank(i);
        }
        toRank(sum);

    }

   

    private void toRank(double[] sum) {
        // int[]r= ArrayUtils.sort(ranking);
        int[] order = ArrayUtils.sort(sum);
        ranking = new int[order.length];
        // som[i] bevat de som van alle ranks die attribuut i gehaald heeft
        //
        // lage values zijn beter
        for (int i = 0; i < order.length; i++) {
            ranking[order[i]] = i;
        }
    }

    public int getRank(int attIndex) {
        return ranking[attIndex];
    }

}