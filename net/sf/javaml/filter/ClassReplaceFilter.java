/**
 * %SVN.HEADER%
 */
package net.sf.javaml.filter;

import net.sf.javaml.core.Instance;

/**
 * Replaces a certain class value with another one.
 * 
 * {@jmlSource}
 * 
 * 
 * @version %SVN.REVISION%
 * 
 * @author Thomas Abeel
 * 
 */
public class ClassReplaceFilter extends AbstractFilter {

    private Object oldClassValue, newClassValue;

    public ClassReplaceFilter(Object oldValue, Object newClassValue) {
        this.newClassValue = newClassValue;
        this.oldClassValue = oldValue;
    }

    @Override
    public void filterInstance(Instance inst) {
        if (inst.classValue().equals(oldClassValue))
            inst.setClassValue(newClassValue);

    }

}