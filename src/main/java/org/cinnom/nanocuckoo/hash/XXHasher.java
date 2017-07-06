package org.cinnom.nanocuckoo.hash;

import net.jpountz.xxhash.XXHashFactory;

/**
 * XXHash 64-bit bucket hasher. Very fast (non-cryptographic) hash with a good distribution.
 */
public class XXHasher implements BucketHasher {

	private final XXHashFactory factory = XXHashFactory.fastestInstance();

	private final int seed;

	/**
	 * Instantiate the XXHasher with the given random seed.
	 * 
	 * @param seed
	 *            Random seed.
	 */
	public XXHasher( int seed ) {

		this.seed = seed;
	}

	/**
	 * Get 64-bit bucket hash using XXHash.
	 * 
	 * @param data
	 *            Data to hash.
	 * @return 64-bit hash.
	 */
	@Override
	public long getHash( byte[] data ) {

		return factory.hash64().hash( data, 0, data.length, seed );
	}
}
