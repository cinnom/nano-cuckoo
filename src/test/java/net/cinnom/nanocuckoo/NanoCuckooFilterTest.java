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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;

import net.cinnom.nanocuckoo.encode.StringEncoder;
import net.cinnom.nanocuckoo.hash.BucketHasher;
import net.cinnom.nanocuckoo.hash.FingerprintHasher;

/**
 * NanoCuckooFilter tests. These are really integration tests, but it runs quickly enough to be a unit test. I'll
 * replace it with a real unit test at some point...
 */
public class NanoCuckooFilterTest {

	@Test
	public void generalUsageTest() {

		long capacity = 32;

		// Use Builder to create a NanoCuckooFilter. Only required parameter is capacity.
		final NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( capacity ).withCountingEnabled( true ) // Enable counting
				.build();

		Assert.assertEquals( capacity, cuckooFilter.getCapacity() );

		String testValue = "test value";

		// Insert a value into the filter
		cuckooFilter.insert( testValue );

		// Check that the value is in the filter
		boolean isValueInFilter = cuckooFilter.contains( testValue ); // Returns true

		Assert.assertTrue( isValueInFilter );

		// Check that some other value is in the filter
		isValueInFilter = cuckooFilter.contains( "some other value" ); // Should return false, probably

		// Generally wouldn't want to assert this, but it will be fine here
		Assert.assertFalse( isValueInFilter );

		// Insert the same value a couple more times
		cuckooFilter.insert( testValue );
		cuckooFilter.insert( testValue );

		// Get a count of how many times the value is in the filter
		int insertedCount = cuckooFilter.count( testValue ); // Returns 3 since we inserted three times with counting enabled

		Assert.assertEquals( 3, insertedCount );

		// Delete value from the filter once
		boolean wasDeleted = cuckooFilter.delete( testValue ); // Returns true since a value was deleted

		Assert.assertTrue( wasDeleted );

		// Try to delete the value up to six more times
		int deletedCount = cuckooFilter.delete( testValue, 6 ); // Returns 2 since only two copies of the value were left

		Assert.assertEquals( 2, deletedCount );

		isValueInFilter = cuckooFilter.contains( testValue ); // Returns false since all copies of the value were deleted

		Assert.assertFalse( isValueInFilter );

		// Double filter capacity by doubling entries per bucket. However, this also roughly doubles max FPP.
		cuckooFilter.expand();

		Assert.assertEquals( capacity * 2, cuckooFilter.getCapacity() );

		// Close the filter when finished with it.
		cuckooFilter.close();
	}

	@Test
	public void multiInsertDeleteCountTest() {

		long capacity = 32;

		final NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( capacity ).withConcurrency( 1 )
				.withCountingEnabled( true ).withConcurrentSwapSafety( ConcurrentSwapSafety.FAST ).build();

		for ( int i = 0; i < 9; i++ ) {
			cuckooFilter.insert( 16384 );
		}

		Assert.assertEquals( 9, cuckooFilter.count( 16384 ) );
		Assert.assertEquals( 9, cuckooFilter.delete( 16384, 9 ) );
	}

	@Test
	public void multiInsertDeleteTest() {

		long capacity = 32;

		final NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( capacity ).withConcurrency( 1 )
				.withCountingEnabled( true ).withConcurrentSwapSafety( ConcurrentSwapSafety.FAST ).build();

		for ( int i = 0; i < 9; i++ ) {
			Assert.assertTrue( cuckooFilter.insert( 0 ) );
		}
		for ( int i = 0; i < 9; i++ ) {
			Assert.assertTrue( cuckooFilter.delete( 0 ) );
		}
	}

	@Test
	public void deleteFalseTest() {

		long capacity = 32;

		final NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( capacity ).withConcurrency( 1 )
				.withCountingEnabled( true ).withConcurrentSwapSafety( ConcurrentSwapSafety.FAST )
				.withFingerprintBits( 32 ).build();

		for ( int i = 0; i < 9; i++ ) {
			Assert.assertTrue( cuckooFilter.insert( i ) );
		}
		for ( int i = 9; i < 18; i++ ) {
			Assert.assertFalse( cuckooFilter.delete( i ) );
		}
	}

	@Test
	public void getMemoryUsageBytesTest() {

		BucketHasher bucketHasher = mock( BucketHasher.class );
		FingerprintHasher fingerprintHasher = mock( FingerprintHasher.class );
		StringEncoder stringEncoder = mock( StringEncoder.class );
		KickedValues kickedValues = mock( KickedValues.class );
		UnsafeBuckets unsafeBuckets = mock( UnsafeBuckets.class );
		BucketLocker bucketLocker = mock( BucketLocker.class );
		Swapper swapper = mock( Swapper.class );

		long memoryUsageBytes = 1024L;

		when( unsafeBuckets.getMemoryUsageBytes() ).thenReturn( memoryUsageBytes );

		final NanoCuckooFilter cuckooFilter = new NanoCuckooFilter( 8, bucketHasher, fingerprintHasher, stringEncoder,
				kickedValues, unsafeBuckets, bucketLocker, swapper );

		Assert.assertEquals( memoryUsageBytes, cuckooFilter.getMemoryUsageBytes() );
	}

	@Test
	public void getLoadFactorTest() {

		BucketHasher bucketHasher = mock( BucketHasher.class );
		FingerprintHasher fingerprintHasher = mock( FingerprintHasher.class );
		StringEncoder stringEncoder = mock( StringEncoder.class );
		KickedValues kickedValues = mock( KickedValues.class );
		UnsafeBuckets unsafeBuckets = mock( UnsafeBuckets.class );
		BucketLocker bucketLocker = mock( BucketLocker.class );
		Swapper swapper = mock( Swapper.class );

		long insertedCount = 512L;
		long capacity = 1024L;

		when( unsafeBuckets.getInsertedCount() ).thenReturn( insertedCount );
		when( unsafeBuckets.getCapacity() ).thenReturn( capacity );

		final NanoCuckooFilter cuckooFilter = new NanoCuckooFilter( 8, bucketHasher, fingerprintHasher, stringEncoder,
				kickedValues, unsafeBuckets, bucketLocker, swapper );

		Assert.assertEquals( (double) insertedCount / capacity, cuckooFilter.getLoadFactor(), 0.000001 );
	}

	@Test
	public void deleteKickedTest() {

		BucketHasher bucketHasher = mock( BucketHasher.class );
		FingerprintHasher fingerprintHasher = mock( FingerprintHasher.class );
		StringEncoder stringEncoder = mock( StringEncoder.class );
		KickedValues kickedValues = mock( KickedValues.class );
		UnsafeBuckets unsafeBuckets = mock( UnsafeBuckets.class );
		BucketLocker bucketLocker = mock( BucketLocker.class );
		Swapper swapper = mock( Swapper.class );

		when( unsafeBuckets.getBucket( anyLong() ) ).thenReturn( 1L );
		when( fingerprintHasher.getHash( anyInt() ) ).thenReturn( 1L );
		when( unsafeBuckets.delete( anyLong(), anyInt() ) ).thenReturn( false );
		when( kickedValues.equals( anyInt(), anyLong(), anyLong() ) ).thenReturn( true );
		when( kickedValues.isClear() ).thenReturn( true );

		final NanoCuckooFilter cuckooFilter = new NanoCuckooFilter( 8, bucketHasher, fingerprintHasher, stringEncoder,
				kickedValues, unsafeBuckets, bucketLocker, swapper );

		cuckooFilter.delete( 1 );

		verify( kickedValues ).clear();
	}

	@Test
	public void deallocatorTest() {

		final UnsafeBuckets unsafeBuckets = mock( UnsafeBuckets.class );

		NanoCuckooFilter.Deallocator deallocator = new NanoCuckooFilter.Deallocator( unsafeBuckets );

		deallocator.run();

		verify( unsafeBuckets ).close();
	}
}
