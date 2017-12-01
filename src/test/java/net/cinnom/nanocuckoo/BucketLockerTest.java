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

import org.junit.Assert;
import org.junit.Test;

/**
 * BucketLocker tests.
 */
public class BucketLockerTest {

	@Test
	public void concurrenyLockUnlockTest() {

		int buckets = 128;
		int concurrency = 64;

		final BucketLocker bucketLocker = new BucketLocker( concurrency, buckets );

		for ( int i = 0; i < concurrency * 10; i++ ) {
			bucketLocker.lockBucket( i );
			bucketLocker.unlockBucket( i );
		}
	}

	@Test
	public void lockUnlockAllTest() {

		int buckets = 128;
		int concurrency = 64;

		final BucketLocker bucketLocker = new BucketLocker( concurrency, buckets );

		bucketLocker.lockAllBuckets();
		bucketLocker.unlockAllBuckets();
	}

	@Test
	public void bucketBoundTest() {

		int buckets = 32;
		int concurrency = 64;

		final BucketLocker bucketLocker = new BucketLocker( concurrency, buckets );

		bucketLocker.lockBucket( 33 );
		bucketLocker.unlockBucket( 1 );
	}

	@Test
	public void unlockFailTest() {

		final int buckets = 128;
		final int concurrency = 64;

		final BucketLocker bucketLocker = new BucketLocker( concurrency, buckets );

		try {
			bucketLocker.unlockBucket( 1 );
			Assert.fail();
		} catch ( IllegalMonitorStateException e ) {
		}
	}

	@Test
	public void unlockAllFailTest() {

		final int buckets = 128;
		final int concurrency = 64;

		final BucketLocker bucketLocker = new BucketLocker( concurrency, buckets );

		try {
			bucketLocker.unlockAllBuckets();
			Assert.fail();
		} catch ( IllegalMonitorStateException e ) {
		}
	}
}
