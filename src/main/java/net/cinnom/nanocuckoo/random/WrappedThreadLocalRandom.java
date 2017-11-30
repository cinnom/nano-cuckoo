package net.cinnom.nanocuckoo.random;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Random int provider that just wraps {@link ThreadLocalRandom}.
 */
public class WrappedThreadLocalRandom implements RandomInt {

	@Override
	public int nextInt() {

		return ThreadLocalRandom.current().nextInt();
	}
}
