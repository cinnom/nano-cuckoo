package org.cinnom.nanocuckoo;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides functionality for locking groups of buckets.
 */
class BucketLocker {

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

}
