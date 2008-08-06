/*
 * TestRemoveAttributes.java 
 * -----------------------
 * Copyright (C) 2008  Thomas Abeel
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * 
 * Author: Thomas Abeel
 */
package junit.filter;

import java.util.HashSet;
import java.util.Set;

import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.filter.RemoveAttributes;

import org.junit.Assert;
import org.junit.Test;

public class TestRemoveAttributes {

    @Test
    public void testRemoveAttributes() {
        Instance i = new DenseInstance(new double[] { 1, 2, 3, 4, 5});
        Set<Integer>toRemove=new HashSet<Integer>();
        toRemove.add(1);
        toRemove.add(4);
        RemoveAttributes ra=new RemoveAttributes(toRemove);
        ra.filterInstance(i);
        Assert.assertTrue(i.noAttributes()==3);
        Assert.assertTrue(i.value(0)==1);
        Assert.assertTrue(i.value(1)==3);
        Assert.assertTrue(i.value(2)==4);
        
        

    }
}