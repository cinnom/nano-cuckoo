/*
 * Copyright 2017 Randall Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.cinnom.nanocuckoo;

import net.cinnom.nanocuckoo.hash.FingerprintHasher;
import net.cinnom.nanocuckoo.random.RandomInt;

/**
 * Bucket swapper. Locks the kicked values during swaps.
 */
class Swapper {

	private final KickedValues kickedValues;
	private final BucketLocker bucketLocker;
	private final UnsafeBuckets buckets;
	private final FingerprintHasher fpHasher;
	private final int maxKicks;
	private final RandomInt randomInt;

	Swapper( final KickedValues kickedValues, final BucketLocker bucketLocker, final UnsafeBuckets buckets,
			final FingerprintHasher fpHasher, final int maxKicks, RandomInt randomInt ) {

		this.kickedValues = kickedValues;
		this.bucketLocker = bucketLocker;
		this.buckets = buckets;
		this.fpHasher = fpHasher;
		this.maxKicks = maxKicks;
		this.randomInt = randomInt;
	}

	boolean swap( int fingerprint, long bucket ) {

		try {
			kickedValues.lock();

			if ( kickedValues.compareAndSetKickedFingerprint( fingerprint ) ) {

				kickedValues.setKickedBucket( bucket );

				for ( int n = 0; n < maxKicks; n++ ) {

					int entrySwap = randomInt.nextInt() & buckets.getEntryMask();

					try {
						bucketLocker.lockBucket( bucket );
						fingerprint = buckets.swap( entrySwap, bucket, fingerprint );
						kickedValues.setKickedFingerprint( fingerprint );
					} finally {
						bucketLocker.unlockBucket( bucket );
					}

					bucket = bucket ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );
					kickedValues.setKickedBucket( bucket );

					try {
						bucketLocker.lockBucket( bucket );
						if ( buckets.insert( bucket, fingerprint ) ) {
							kickedValues.clear();
							return true;
						}
					} finally {
						bucketLocker.unlockBucket( bucket );
					}
				}

				buckets.incrementInsertedCount(); // increase count when kicked value set

				return true;
			}

		} finally {
			kickedValues.unlock();
		}

		return false;
	}

	int getMaxKicks() {

		return maxKicks;
	}

	RandomInt getRandomInt() {

		return randomInt;
	}
}
