package org.cinnom.nanocuckoo.hash;

/**
 * Results in about 0.5% worse occupancy compared to XXHasher for fingerprints, but much better throughput.
 *
 * Created by cinnom on 6/30/2017.
 */
public final class FixedHasher implements FingerprintHasher {

    private static final long MURMUR_HASH_MIX = 0xC4CEB9fE1A85EC53L;

    @Override
    public long getHash( int value ) {

        return value * MURMUR_HASH_MIX;
    }
}
