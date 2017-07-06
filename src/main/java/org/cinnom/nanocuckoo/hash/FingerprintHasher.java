package org.cinnom.nanocuckoo.hash;

/**
 * 64-bit Hasher for hashing fingerprint values.
 */
public interface FingerprintHasher {

	/**
	 * Hash a fingerprint value into 64 bits.
	 * 
	 * @param value
	 *            Value to hash.
	 * @return 64-bit hash.
	 */
	long getHash( int value );

}
