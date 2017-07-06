package org.cinnom.nanocuckoo;

import org.cinnom.nanocuckoo.hash.FingerprintHasher;

/**
 * "Smart" bucket swapper. Uses FastSwapper until a specified load factor is hit, then uses ReliableSwapper.
 */
public class SmartSwapper implements Swapper {

	private final FastSwapper fastSwapper;
	private final ReliableSwapper reliableSwapper;
	private final long maxFastCount;
	private final UnsafeBuckets buckets;

	SmartSwapper( final KickedValues kickedValues, final BucketLocker bucketLocker, final UnsafeBuckets buckets,
			final FingerprintHasher fpHasher, final int maxKicks, final int randomSeed, double smartInsertLoadFactor ) {

		fastSwapper = new FastSwapper( kickedValues, bucketLocker, buckets, fpHasher, maxKicks, randomSeed );
		reliableSwapper = new ReliableSwapper( kickedValues, bucketLocker, buckets, fpHasher, maxKicks, randomSeed );

		this.maxFastCount = (long) ( buckets.getCapacity() * smartInsertLoadFactor );
		this.buckets = buckets;
	}

	@Override
	public boolean swap( int fingerprint, long bucket ) {

		if ( buckets.getInsertedCount() > maxFastCount ) {
			return reliableSwapper.swap( fingerprint, bucket );
		}

		return fastSwapper.swap( fingerprint, bucket );
	}
}
