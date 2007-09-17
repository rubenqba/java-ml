/**
 * FastFourierTransformTest.java
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
package junit.filter;

import net.sf.javaml.core.Instance;
import net.sf.javaml.core.SimpleInstance;
import net.sf.javaml.filter.FastFourierTransform;
import net.sf.javaml.utils.MathUtils;

import org.junit.Assert;
import org.junit.Test;


public class FastFourierTransformTest {

    @Test
    public void testFFT(){
        double[] t={1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
        Instance tmp=new SimpleInstance(t);
        FastFourierTransform fft=new FastFourierTransform();
        Instance out=fft.filterInstance(tmp);
        System.out.println(out);
        for(int i=1;i<16;i++){
            Assert.assertTrue(MathUtils.eq(out.getComplex(i).re,-8));
        }
        
    }

    @Test
    public void testIFFT(){
        double[] t={1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
        Instance tmp=new SimpleInstance(t);
        FastFourierTransform fft=new FastFourierTransform();
        Instance out=fft.unfilterInstance(tmp);
        System.out.println(out);
        for(int i=1;i<16;i++){
            Assert.assertTrue(MathUtils.eq(out.getComplex(i).re,-0.5));
        }
        
    }
    
}
