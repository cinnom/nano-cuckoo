package org.cinnom.nanocuckoo.hash;

/**
 * 64-bit Hasher for hashing initial bucket indices.
 */
public interface BucketHasher {

    /**
     * Hash data into 64 bits.
     * @param data Data to hash.
     * @return 64-bit hash.
     */
    long getHash(final byte[] data);
}
