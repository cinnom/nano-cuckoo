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

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides functionality for locking groups of buckets.
 */
class BucketLocker implements Serializable {

	private static final long serialVersionUID = 1L;

	private final ReentrantLock[] bucketLocks;
	private final int concurrencyBucketMask;

	BucketLocker( int concurrency, long bucketCount ) {

		// Set concurrency mask for setting bucket locks
		if ( bucketCount < Integer.MAX_VALUE ) {
			concurrency = Math.min( concurrency, (int) bucketCount );
		}
		this.concurrencyBucketMask = concurrency - 1;

		// Initialize bucket locks
		bucketLocks = new ReentrantLock[concurrency];
		for ( int i = 0; i < concurrency; i++ ) {
			bucketLocks[i] = new ReentrantLock();
		}
	}

	void lockBucket( long bucket ) {

		bucketLocks[(int) bucket & concurrencyBucketMask].lock();
	}

	void unlockBucket( long bucket ) {

		bucketLocks[(int) bucket & concurrencyBucketMask].unlock();
	}

	void lockAllBuckets() {

		for ( ReentrantLock bucketLock : bucketLocks ) {
			bucketLock.lock();
		}
	}

	void unlockAllBuckets() {

		for ( ReentrantLock bucketLock : bucketLocks ) {
			bucketLock.unlock();
		}
	}

	int getConcurrency() {

		return concurrencyBucketMask + 1;
	}
}
