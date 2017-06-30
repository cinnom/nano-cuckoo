package org.cinnom.nanocuckoo.random;

import java.util.SplittableRandom;

/**
 * Created by rjones on 6/29/17.
 */
public class SplittableRandomWrapper implements Randomizer {

	private SplittableRandom random;

	public SplittableRandomWrapper(long seed) {

		random = new SplittableRandom( seed );
	}

	public int nextInt() {

		return random.nextInt();
	}
}
