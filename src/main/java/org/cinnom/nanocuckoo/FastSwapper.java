package org.cinnom.nanocuckoo;

import java.util.SplittableRandom;

import org.cinnom.nanocuckoo.hash.FingerprintHasher;

/**
 * Fast bucket swapper. Doesn't lock during swapping; sets the kicked value afterwards if insert failed. Can lose a
 * number of previously inserted values up to (concurrent insertion threads - 1) when the filter hits max load.
 */
class FastSwapper implements Swapper {

	private final KickedValues kickedValues;
	private final BucketLocker bucketLocker;
	private final SplittableRandom random;
	private final UnsafeBuckets buckets;
	private final FingerprintHasher fpHasher;
	private final int maxKicks;

	FastSwapper( final KickedValues kickedValues, final BucketLocker bucketLocker, final UnsafeBuckets buckets,
			final FingerprintHasher fpHasher, final int maxKicks, final int randomSeed ) {

		this.kickedValues = kickedValues;
		this.bucketLocker = bucketLocker;
		this.buckets = buckets;
		this.fpHasher = fpHasher;
		this.maxKicks = maxKicks;
		random = new SplittableRandom( randomSeed );
	}

	@Override
	public boolean swap( int fingerprint, long bucket ) {

		if(kickedValues.isClear()) {

			for ( int n = 0; n < maxKicks; n++ ) {

				int entrySwap = random.nextInt() & buckets.getEntryMask();

				try {
					bucketLocker.lockBucket( bucket );
					fingerprint = buckets.swap( entrySwap, bucket, fingerprint );
				} finally {
					bucketLocker.unlockBucket( bucket );
				}

				bucket = bucket ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

				try {
					bucketLocker.lockBucket( bucket );
					if ( buckets.insert( bucket, fingerprint ) ) {
						return true;
					}
				} finally {
					bucketLocker.unlockBucket( bucket );
				}
			}

			if ( kickedValues.compareAndSetKickedFingerprint( -1, fingerprint ) ) {
				kickedValues.setKickedBucket( bucket );
				buckets.incrementInsertedCount(); // increase count when kicked value set
				return true;
			}
		}

		return false;
	}
}
