package org.cinnom.nanocuckoo;

import java.util.SplittableRandom;

import org.cinnom.nanocuckoo.hash.FingerprintHasher;

/**
 * Reliable bucket swapper (default). Locks the kicked values during swaps.
 */
class ReliableSwapper implements Swapper {

	private final KickedValues kickedValues;
	private final BucketLocker bucketLocker;
	private final SplittableRandom random;
	private final UnsafeBuckets buckets;
	private final FingerprintHasher fpHasher;
	private final int maxKicks;

	ReliableSwapper( final KickedValues kickedValues, final BucketLocker bucketLocker, final UnsafeBuckets buckets,
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

		try {
			kickedValues.lock();

			if ( kickedValues.compareAndSetKickedFingerprint( -1, fingerprint ) ) {

				kickedValues.setKickedBucket( bucket );

				for ( int n = 0; n < maxKicks; n++ ) {

					int entrySwap = random.nextInt() & buckets.getEntryMask();

					try {
						bucketLocker.lockBucket( kickedValues.getKickedBucket() );
						kickedValues.setKickedFingerprint( buckets.swap( entrySwap, kickedValues.getKickedBucket(),
								kickedValues.getKickedFingerprint() ) );
					} finally {
						bucketLocker.unlockBucket( kickedValues.getKickedBucket() );
					}

					kickedValues.setKickedBucket( kickedValues.getKickedBucket()
							^ buckets.getBucket( fpHasher.getHash( kickedValues.getKickedFingerprint() ) ) );

					bucketLocker.lockBucket( kickedValues.getKickedBucket() );
					if ( buckets.insert( kickedValues.getKickedBucket(), kickedValues.getKickedFingerprint() ) ) {
						bucketLocker.unlockBucket( kickedValues.getKickedBucket() );
						kickedValues.clear();
						return true;
					}
					bucketLocker.unlockBucket( kickedValues.getKickedBucket() );

				}

				buckets.incrementInsertedCount(); // increase count when kicked value set

				return true;
			}

		} finally {
			kickedValues.unlock();
		}

		return false;
	}
}
