package org.cinnom.nanocuckoo;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Kicked value tracker. New inserts can't occur if this is populated. Provides locking for inserts/deletes.
 */
class KickedValues {

	private final ReentrantLock lock = new ReentrantLock();
	private final AtomicInteger kickedFingerprint = new AtomicInteger( -1 );
	private volatile long kickedBucket = -1;

	int getKickedFingerprint() {

		return kickedFingerprint.get();
	}

	void setKickedFingerprint( int kickedFingerprint ) {

		this.kickedFingerprint.set( kickedFingerprint );
	}

	boolean compareAndSetKickedFingerprint( int expected, int kickedFingerprint ) {

		return this.kickedFingerprint.compareAndSet( expected, kickedFingerprint);
	}

	long getKickedBucket() {

		return kickedBucket;
	}

	void setKickedBucket( long kickedBucket ) {

		this.kickedBucket = kickedBucket;
	}

	boolean equals( int fingerprint, long bucket1, long bucket2 ) {

		return ( bucket1 == kickedBucket || bucket2 == kickedBucket ) && fingerprint == kickedFingerprint.get();
	}

	void clear() {

		kickedFingerprint.set( -1 );
		kickedBucket = -1;
	}

	boolean isClear() {

		return kickedFingerprint.get() == -1;
	}

	void lock() {

		lock.lock();
	}

	void unlock() {

		lock.unlock();
	}

}
