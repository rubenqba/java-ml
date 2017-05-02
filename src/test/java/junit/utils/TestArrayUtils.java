/**
 * %SVN.HEADER%
 */
package junit.utils;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import net.sf.javaml.utils.ArrayUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;

public class TestArrayUtils {

    @Test
    public void testArrayUtils() {
        double[] arr = {1, 5, 4, 3, 8, 10, 2, 6};
        int[] order = ArrayUtils.sort(arr);
        assertThat(Ints.asList(order), contains(0, 6, 3, 2, 1, 7, 4, 5));
    }

    @Test
    public void testReverse() {
        double[] arr = {1d, 2d, 3d, 4d, 5d};
        ArrayUtils.reverse(arr);
        assertThat(Doubles.asList(arr), hasItems(5d, 4d, 3d, 2d, 1d));

        int[] arr2 = new int[]{1, 2, 3, 4};
        ArrayUtils.reverse(arr2);
        assertThat(Ints.asList(arr2), hasItems(4, 3, 2, 1));
    }
}
