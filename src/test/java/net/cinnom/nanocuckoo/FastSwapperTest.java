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
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.SplittableRandom;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
		final int maxKicks = 400;
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

		fastSwapper.swap( fingerprint, bucket );

		verify( unsafeBuckets ).insert( swappedBucket2, swappedFingerprint );
	}
}
