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

		for(int i = 0; i < concurrency * 10; i++) {
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
			Assert.assertTrue( false );
		} catch (IllegalMonitorStateException e) {
		}
	}

	@Test
	public void unlockAllFailTest() {

		final int buckets = 128;
		final int concurrency = 64;

		final BucketLocker bucketLocker = new BucketLocker( concurrency, buckets );

		try {
			bucketLocker.unlockAllBuckets();
			Assert.assertTrue( false );
		} catch (IllegalMonitorStateException e) {
		}
	}
}
