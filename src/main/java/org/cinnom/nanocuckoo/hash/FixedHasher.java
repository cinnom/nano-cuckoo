package org.cinnom.nanocuckoo.hash;

/**
 * Hashes fingerprints by multiplying them by a fixed value. Results in about 0.5% worse occupancy compared to XXHasher
 * for fingerprints, but much better throughput. Credit goes to CuckooFilter4J for this idea.
 */
public final class FixedHasher implements FingerprintHasher {

	private static final long MURMUR_HASH_MIX = 0xC4CEB9FE1A85EC53L;

	/**
	 * "Hash" a fingerprint by multiplying it by 0xC4CEB9fE1A85EC53L.
	 * 
	 * @param value
	 *            Value to hash.
	 * @return 64-bit hash.
	 */
	@Override
	public long getHash( int value ) {

		return value * MURMUR_HASH_MIX;
	}
}
