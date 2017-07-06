package org.cinnom.nanocuckoo.hash;

import net.jpountz.xxhash.XXHashFactory;

/**
 * XXHash bucket hasher. Fast.
 */
public class XXHasher implements BucketHasher {

	private final XXHashFactory factory = XXHashFactory.fastestInstance();

	private int seed;

	/**
	 * Instantiate the XXHasher with the given random seed.
	 * @param seed Random seed.
	 */
	public XXHasher( int seed ) {

		this.seed = seed;
	}

	/**
	 * Get 64-bit bucket hash using XXHash.
	 * @param data Data to hash.
	 * @return 64-bit hash.
	 */
	@Override
	public long getHash( byte[] data ) {

		return factory.hash64().hash( data, 0, data.length, seed );
	}
}
