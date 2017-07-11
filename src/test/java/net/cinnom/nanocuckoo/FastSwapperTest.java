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
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.SplittableRandom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FastSwapper tests.
 */
public class FastSwapperTest {

	@Test
	public void basicSwapInsertTest() {

		final KickedValues kickedValues = mock( KickedValues.class );
		final BucketLocker bucketLocker = mock( BucketLocker.class );
		final UnsafeBuckets unsafeBuckets = mock( UnsafeBuckets.class );
		final FingerprintHasher fingerprintHasher = mock( FingerprintHasher.class );
		final SplittableRandom random = mock( SplittableRandom.class );
		final int maxKicks = 5;
		final int randomSeed = 0x48F7E28A;

		final int fingerprint = 5;
		final long bucket = 37;

		final int nextInt = 1;
		final int swappedFingerprint = 117;
		final long swappedFpHash = 123456789L;
		final long swappedBucket = 12L;
		final long swappedBucket2 = bucket ^ swappedBucket;

		when( kickedValues.isClear() ).thenReturn( true );
		when( random.nextInt() ).thenReturn( nextInt );
		when( unsafeBuckets.getEntryMask() ).thenReturn( 3 );
		when( unsafeBuckets.swap( nextInt, bucket, fingerprint ) ).thenReturn( swappedFingerprint );
		when( fingerprintHasher.getHash( swappedFingerprint ) ).thenReturn( swappedFpHash );
		when( unsafeBuckets.getBucket( swappedFpHash )).thenReturn( swappedBucket );
		when( unsafeBuckets.insert( swappedBucket2, swappedFingerprint ) ).thenReturn( true );

		final FastSwapper fastSwapper = new FastSwapper( kickedValues, bucketLocker, unsafeBuckets, fingerprintHasher, maxKicks, randomSeed, random );

		Assert.assertTrue(fastSwapper.swap( fingerprint, bucket ));

		verify( unsafeBuckets ).insert( swappedBucket2, swappedFingerprint );
	}

	@Test
	public void cantInsertTest() {

		final KickedValues kickedValues = mock( KickedValues.class );
		final BucketLocker bucketLocker = mock( BucketLocker.class );
		final UnsafeBuckets unsafeBuckets = mock( UnsafeBuckets.class );
		final FingerprintHasher fingerprintHasher = mock( FingerprintHasher.class );
		final SplittableRandom random = mock( SplittableRandom.class );
		final int maxKicks = 5;
		final int randomSeed = 0x48F7E28A;

		final int fingerprint = 5;
		final long bucket = 37;

		when( kickedValues.isClear() ).thenReturn( false );

		final FastSwapper fastSwapper = new FastSwapper( kickedValues, bucketLocker, unsafeBuckets, fingerprintHasher, maxKicks, randomSeed, random );

		Assert.assertFalse( fastSwapper.swap( fingerprint, bucket ) );

		verify( unsafeBuckets, never() ).insert( anyLong(), anyInt() );
		verify( unsafeBuckets, never() ).swap( anyInt(), anyLong(), anyInt() );
		verify( kickedValues, never() ).compareAndSetKickedFingerprint( anyInt() );
	}

	@Test
	public void maxKicksTest() {

		final KickedValues kickedValues = mock( KickedValues.class );
		final BucketLocker bucketLocker = mock( BucketLocker.class );
		final UnsafeBuckets unsafeBuckets = mock( UnsafeBuckets.class );
		final FingerprintHasher fingerprintHasher = mock( FingerprintHasher.class );
		final SplittableRandom random = mock( SplittableRandom.class );
		final int maxKicks = 5;
		final int randomSeed = 0x48F7E28A;

		final int fingerprint = 5;
		final long bucket = 37;

		final int nextInt = 1;
		final int swappedFingerprint = 117;
		final long swappedFpHash = 123456789L;
		final long swappedBucket = 12L;
		final long swappedBucket2 = bucket ^ swappedBucket;

		when( kickedValues.isClear() ).thenReturn( true );
		when( random.nextInt() ).thenReturn( nextInt );
		when( unsafeBuckets.getEntryMask() ).thenReturn( 3 );
		when( unsafeBuckets.swap( anyInt(), anyLong(), anyInt() ) ).thenReturn( swappedFingerprint );
		when( fingerprintHasher.getHash( swappedFingerprint ) ).thenReturn( swappedFpHash );
		when( unsafeBuckets.getBucket( swappedFpHash )).thenReturn( swappedBucket );
		when( unsafeBuckets.insert( anyLong(), anyInt() ) ).thenReturn( false );
		when( kickedValues.compareAndSetKickedFingerprint( swappedFingerprint ) ).thenReturn( true );

		final FastSwapper fastSwapper = new FastSwapper( kickedValues, bucketLocker, unsafeBuckets, fingerprintHasher, maxKicks, randomSeed, random );

		Assert.assertTrue( fastSwapper.swap( fingerprint, bucket ) );

		verify( bucketLocker, times(10 ) ).lockBucket( anyLong() );
		verify( bucketLocker, times(10 ) ).unlockBucket( anyLong() );
		verify( unsafeBuckets, times(5 ) ).insert( anyLong(), anyInt() );
		verify( unsafeBuckets, times(5 ) ).swap( anyInt(), anyLong(), anyInt() );
		verify( kickedValues ).compareAndSetKickedFingerprint( swappedFingerprint );
		verify( kickedValues ).setKickedBucket( anyLong() );
		verify( unsafeBuckets ).incrementInsertedCount();
	}
}
