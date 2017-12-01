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

	boolean compareAndSetKickedFingerprint( int kickedFingerprint ) {

		return this.kickedFingerprint.compareAndSet( -1, kickedFingerprint );
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
