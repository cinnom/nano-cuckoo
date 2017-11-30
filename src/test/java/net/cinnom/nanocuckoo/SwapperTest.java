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
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FastSwapper tests.
 */
public class SwapperTest {

	@Test
	public void basicSwapInsertTest() {

		final KickedValues kickedValues = new KickedValues();
		final BucketLocker bucketLocker = mock( BucketLocker.class );
		final UnsafeBuckets unsafeBuckets = mock( UnsafeBuckets.class );
		final FingerprintHasher fingerprintHasher = mock( FingerprintHasher.class );
		final RandomInt randomInt = mock( RandomInt.class );
		final int maxKicks = 5;

		final int fingerprint = 5;
		final long bucket = 37;

		final int nextInt = 1;
		final int swappedFingerprint = 117;
		final long swappedFpHash = 123456789L;
		final long swappedBucket = 12L;
		final long swappedBucket2 = bucket ^ swappedBucket;

		when( unsafeBuckets.getEntryMask() ).thenReturn( 3 );
		when( randomInt.nextInt() ).thenReturn( nextInt );
		when( unsafeBuckets.swap( nextInt, bucket, fingerprint ) ).thenReturn( swappedFingerprint );
		when( fingerprintHasher.getHash( swappedFingerprint ) ).thenReturn( swappedFpHash );
		when( unsafeBuckets.getBucket( swappedFpHash ) ).thenReturn( swappedBucket );
		when( unsafeBuckets.insert( swappedBucket2, swappedFingerprint ) ).thenReturn( true );

		final Swapper swapperSwapper = new Swapper( kickedValues, bucketLocker, unsafeBuckets, fingerprintHasher,
				maxKicks, randomInt );

		Assert.assertTrue( swapperSwapper.swap( fingerprint, bucket ) );

		verify( unsafeBuckets ).insert( swappedBucket2, swappedFingerprint );
		Assert.assertTrue( kickedValues.isClear() );
	}

	@Test
	public void cantInsertTest() {

		final KickedValues kickedValues = mock( KickedValues.class );
		final BucketLocker bucketLocker = mock( BucketLocker.class );
		final UnsafeBuckets unsafeBuckets = mock( UnsafeBuckets.class );
		final FingerprintHasher fingerprintHasher = mock( FingerprintHasher.class );
		final RandomInt randomInt = mock( RandomInt.class );
		final int maxKicks = 5;

		final int fingerprint = 5;
		final long bucket = 37;

		when( kickedValues.compareAndSetKickedFingerprint( fingerprint ) ).thenReturn( false );

		final Swapper swapper = new Swapper( kickedValues, bucketLocker, unsafeBuckets, fingerprintHasher, maxKicks,
				randomInt );

		Assert.assertFalse( swapper.swap( fingerprint, bucket ) );

		verify( unsafeBuckets, never() ).insert( anyLong(), anyInt() );
		verify( unsafeBuckets, never() ).swap( anyInt(), anyLong(), anyInt() );
		verify( kickedValues, never() ).setKickedFingerprint( anyInt() );
		verify( kickedValues, never() ).setKickedBucket( anyLong() );
		verify( kickedValues, never() ).clear();
		verify( kickedValues ).lock();
		verify( kickedValues ).unlock();
	}

	@Test
	public void maxKicksTest() {

		final KickedValues kickedValues = mock( KickedValues.class );
		final BucketLocker bucketLocker = mock( BucketLocker.class );
		final UnsafeBuckets unsafeBuckets = mock( UnsafeBuckets.class );
		final FingerprintHasher fingerprintHasher = mock( FingerprintHasher.class );
		final RandomInt randomInt = mock( RandomInt.class );
		final int maxKicks = 5;

		final int fingerprint = 5;
		final long bucket = 37;

		final int nextInt = 1;
		final int swappedFingerprint = 117;
		final long swappedFpHash = 123456789L;
		final long swappedBucket = 12L;
		final long swappedBucket2 = bucket ^ swappedBucket;

		when( kickedValues.compareAndSetKickedFingerprint( fingerprint ) ).thenReturn( true );
		when( randomInt.nextInt() ).thenReturn( nextInt );
		when( unsafeBuckets.getEntryMask() ).thenReturn( 3 );
		when( unsafeBuckets.swap( anyInt(), anyLong(), anyInt() ) ).thenReturn( swappedFingerprint );
		when( fingerprintHasher.getHash( swappedFingerprint ) ).thenReturn( swappedFpHash );
		when( unsafeBuckets.getBucket( swappedFpHash ) ).thenReturn( swappedBucket );
		when( unsafeBuckets.insert( anyLong(), anyInt() ) ).thenReturn( false );

		final Swapper swapper = new Swapper( kickedValues, bucketLocker, unsafeBuckets, fingerprintHasher, maxKicks,
				randomInt );

		Assert.assertTrue( swapper.swap( fingerprint, bucket ) );

		verify( bucketLocker, times( 10 ) ).lockBucket( anyLong() );
		verify( bucketLocker, times( 10 ) ).unlockBucket( anyLong() );
		verify( unsafeBuckets, times( 5 ) ).insert( anyLong(), anyInt() );
		verify( unsafeBuckets, times( 5 ) ).swap( anyInt(), anyLong(), anyInt() );
		verify( kickedValues, times( 5 ) ).setKickedFingerprint( anyInt() );
		verify( kickedValues, times( 6 ) ).setKickedBucket( anyLong() );
		verify( unsafeBuckets ).incrementInsertedCount();
	}
}
