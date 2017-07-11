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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.SplittableRandom;

import net.cinnom.nanocuckoo.hash.FingerprintHasher;

/**
 * Fast bucket swapper. Doesn't lock during swapping; sets the kicked value afterwards if insert failed. Can lose a
 * number of previously inserted values up to (concurrent insertion threads - 1) when the filter hits max load.
 */
class FastSwapper implements Swapper, Serializable {

	private static final long serialVersionUID = 1L;

	private final KickedValues kickedValues;
	private final BucketLocker bucketLocker;
	private final UnsafeBuckets buckets;
	private final FingerprintHasher fpHasher;
	private final int maxKicks;
	private final int randomSeed;
	private transient SplittableRandom random;

	FastSwapper( final KickedValues kickedValues, final BucketLocker bucketLocker, final UnsafeBuckets buckets,
			final FingerprintHasher fpHasher, final int maxKicks, final int randomSeed, final SplittableRandom random ) {

		this.kickedValues = kickedValues;
		this.bucketLocker = bucketLocker;
		this.buckets = buckets;
		this.fpHasher = fpHasher;
		this.maxKicks = maxKicks;
		this.randomSeed = randomSeed;
		this.random = random;
	}

	@Override
	public boolean swap( int fingerprint, long bucket ) {

		if ( kickedValues.isClear() ) {

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

			if ( kickedValues.compareAndSetKickedFingerprint( fingerprint ) ) {
				kickedValues.setKickedBucket( bucket );
				buckets.incrementInsertedCount(); // increase count when kicked value set
				return true;
			}
		}

		return false;
	}

	private void readObject( ObjectInputStream in ) throws IOException, ClassNotFoundException {

		in.defaultReadObject();
		random = new SplittableRandom( randomSeed );
	}
}
