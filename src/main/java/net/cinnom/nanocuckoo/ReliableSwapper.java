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
 * Reliable bucket swapper (default). Locks the kicked values during swaps.
 */
class ReliableSwapper implements Swapper, Serializable {

	private static final long serialVersionUID = 1L;

	private final KickedValues kickedValues;
	private final BucketLocker bucketLocker;
	private final UnsafeBuckets buckets;
	private final FingerprintHasher fpHasher;
	private final int maxKicks;
	private final int randomSeed;
	private transient SplittableRandom random;

	ReliableSwapper( final KickedValues kickedValues, final BucketLocker bucketLocker, final UnsafeBuckets buckets,
			final FingerprintHasher fpHasher, final int maxKicks, final int randomSeed ) {

		this.kickedValues = kickedValues;
		this.bucketLocker = bucketLocker;
		this.buckets = buckets;
		this.fpHasher = fpHasher;
		this.maxKicks = maxKicks;
		this.randomSeed = randomSeed;
		random = new SplittableRandom( randomSeed );
	}

	@Override
	public boolean swap( int fingerprint, long bucket ) {

		try {
			kickedValues.lock();

			if ( kickedValues.compareAndSetKickedFingerprint( fingerprint ) ) {

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

	private void readObject( ObjectInputStream in ) throws IOException, ClassNotFoundException {

		in.defaultReadObject();
		random = new SplittableRandom( randomSeed );
	}
}
