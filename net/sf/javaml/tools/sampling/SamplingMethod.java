package net.sf.javaml.tools.sampling;

import java.util.List;
import java.util.Random;

/**
 * Defines sampling methods to select a subset of a set integers. The original
 * set may contain duplicates and the output set may contain duplicates.
 * 
 * @author Thomas
 * 
 */
public abstract class SamplingMethod {

	/**
	 * Samples a set of integers and returns a new set of integers that is the
	 * result of the sampling.
	 * 
	 * The returned set will be the same size as the original set.
	 * 
	 * @param set
	 *            the set to sample from
	 * @return the selected of integers
	 */
	public List<Integer> sample(List<Integer> set) {
		return sample(set, set.size());
	}

	/**
	 * Samples a set of integers and returns a new set of integers that is the
	 * result of the sampling.
	 * 
	 * @param set
	 *            the set to sample from
	 * @param size
	 *            the number of items that should be in the returned sample
	 * @return the selected set of integers
	 */
	public List<Integer> sample(List<Integer> set, int size) {
		return sample(set, size, System.currentTimeMillis());
	}

	/**
	 * Samples a set of integers and returns a new set of integers that is the
	 * result of the sampling.
	 * 
	 * @param set
	 *            the set to sample from
	 * @param size
	 *            the number of items that should be in the returned sample
	 * @param seed
	 *            the seed used for the random generator
	 * @return the selected set of integers
	 */
	public abstract List<Integer> sample(List<Integer> set, int size, long seed);

	
}