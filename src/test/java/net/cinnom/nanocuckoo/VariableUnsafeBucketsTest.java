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
 * VariableUnsafeBuckets tests.
 */
public class VariableUnsafeBucketsTest {

	@Test
	public void multiEntriesBucketsTest() {

		final int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = false;

		for ( int fp = 1; fp <= 32; fp++ ) {

			final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
					countingDisabled, 0 );

			for ( int e = 0; e < entries; e++ ) {
				for ( int b = 0; b < buckets; b++ ) {

					int fpMask = -1 >>> ( 32 - fp );
					int value = ( 1023 + e * b ) & fpMask;
					varUnsafeBuckets.putValue( e, b, value );
					Assert.assertEquals( value, varUnsafeBuckets.getValue( e, b ) );
				}
			}
		}
	}

	@Test
	public void swapTest() {

		final int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = false;

		for ( int fp = 1; fp <= 32; fp++ ) {

			final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
					countingDisabled, 0 );

			for ( int e = 0; e < entries; e++ ) {
				for ( int b = 0; b < buckets; b++ ) {

					int fpMask = -1 >>> ( 32 - fp );
					int value = ( 1023 + e * b ) & fpMask;
					int swapValue = ( value + 999 ) & fpMask;
					varUnsafeBuckets.putValue( e, b, value );
					Assert.assertEquals( value, varUnsafeBuckets.swap( e, b, swapValue ) );
					Assert.assertEquals( swapValue, varUnsafeBuckets.getValue( e, b ) );
				}
			}
		}
	}

	@Test
	public void scaleUpTest() {

		final int fp = 13;
		final int entries = 4;
		long buckets = 33;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		buckets = 64;

		long scaledUpCapacity = buckets * entries;
		Assert.assertEquals( scaledUpCapacity, varUnsafeBuckets.getTotalCapacity() );
		Assert.assertEquals( ( fp * scaledUpCapacity ) / 8, varUnsafeBuckets.getMemoryUsageBytes() );
		Assert.assertEquals( buckets, varUnsafeBuckets.getBucketCount() );
	}

	@Test
	public void incrementDecrementCountTest() {

		final int fp = 13;
		final int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		varUnsafeBuckets.incrementInsertedCount();
		varUnsafeBuckets.incrementInsertedCount();
		varUnsafeBuckets.incrementInsertedCount();
		varUnsafeBuckets.incrementInsertedCount();
		varUnsafeBuckets.incrementInsertedCount();

		varUnsafeBuckets.decrementInsertedCount();
		varUnsafeBuckets.decrementInsertedCount();

		Assert.assertEquals( 3, varUnsafeBuckets.getInsertedCount() );
	}

	@Test
	public void expandTest() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		varUnsafeBuckets.expand();
		varUnsafeBuckets.expand();
		entries *= 4;

		long scaledUpCapacity = buckets * entries;
		Assert.assertEquals( scaledUpCapacity, varUnsafeBuckets.getTotalCapacity() );
		Assert.assertEquals( ( fp * scaledUpCapacity ) / 8, varUnsafeBuckets.getMemoryUsageBytes() );
	}

	@Test
	public void insertContainsTest() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		long bucket = 13;
		int value = 2456;

		varUnsafeBuckets.insert( bucket, value );

		Assert.assertTrue( varUnsafeBuckets.contains( bucket, value ) );
	}

	@Test
	public void notContainsTest() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		long bucket = 13;
		int value = 2456;

		varUnsafeBuckets.insert( bucket, value + 1 );

		Assert.assertFalse( varUnsafeBuckets.contains( bucket, value ) );
	}

	@Test
	public void insertDeleteTest() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		long bucket = 13;
		int value = 2456;

		varUnsafeBuckets.insert( bucket, value );

		Assert.assertTrue( varUnsafeBuckets.delete( bucket, value ) );
	}

	@Test
	public void notDeleteTest() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		long bucket = 13;
		int value = 2456;

		varUnsafeBuckets.insert( bucket, value + 1 );

		Assert.assertFalse( varUnsafeBuckets.delete( bucket, value ) );
	}

	@Test
	public void insertCountTest() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = false;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		long bucket = 13;
		int value = 2456;

		varUnsafeBuckets.insert( bucket, value );
		varUnsafeBuckets.insert( bucket, value );
		varUnsafeBuckets.insert( bucket, value );
		varUnsafeBuckets.insert( bucket, value + 1 );
		varUnsafeBuckets.delete( bucket, value );

		Assert.assertEquals( 2, varUnsafeBuckets.count( bucket, value ) );
		Assert.assertEquals( 3, varUnsafeBuckets.getInsertedCount() );
	}

	@Test
	public void insertDeleteCountTest() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = false;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		long bucket = 13;
		int value = 2456;

		varUnsafeBuckets.insert( bucket, value );
		varUnsafeBuckets.insert( bucket, value );
		varUnsafeBuckets.insert( bucket, value );
		varUnsafeBuckets.insert( bucket, value + 1 );

		Assert.assertEquals( 2, varUnsafeBuckets.deleteCount( bucket, value, 2 ) );
		Assert.assertEquals( 2, varUnsafeBuckets.getInsertedCount() );
	}

	@Test
	public void insertNoCountingTest() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		long bucket = 13;
		int value = 2456;

		varUnsafeBuckets.insert( bucket, value );
		varUnsafeBuckets.insert( bucket, value );
		varUnsafeBuckets.insert( bucket, value );

		Assert.assertEquals( 1, varUnsafeBuckets.count( bucket, value ) );
		Assert.assertEquals( 1, varUnsafeBuckets.getInsertedCount() );
	}

	@Test
	public void closedInsertNPE() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		long bucket = 13;
		int value = 2456;

		varUnsafeBuckets.close();

		try {
			varUnsafeBuckets.insert( bucket, value );
			Assert.fail();
		} catch ( NullPointerException ex ) {

		}
	}

	@Test
	public void closedExpandNPE() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		varUnsafeBuckets.close();

		try {
			varUnsafeBuckets.expand();
			Assert.fail();
		} catch ( NullPointerException ex ) {

		}
	}

	@Test
	public void closedContainsNPE() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		long bucket = 13;
		int value = 2456;

		varUnsafeBuckets.close();

		try {
			varUnsafeBuckets.contains( bucket, value );
			Assert.fail();
		} catch ( NullPointerException ex ) {

		}
	}

	@Test
	public void closedCountNPE() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		long bucket = 13;
		int value = 2456;

		varUnsafeBuckets.close();

		try {
			varUnsafeBuckets.count( bucket, value );
			Assert.fail();
		} catch ( NullPointerException ex ) {

		}
	}

	@Test
	public void closedDeleteNPE() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		long bucket = 13;
		int value = 2456;

		varUnsafeBuckets.close();

		try {
			varUnsafeBuckets.delete( bucket, value );
			Assert.fail();
		} catch ( NullPointerException ex ) {

		}
	}

	@Test
	public void closedDeleteCountNPE() {

		final int fp = 13;
		int entries = 4;
		final long buckets = 32;
		final boolean countingDisabled = true;

		final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp,
				countingDisabled, 0 );

		long bucket = 13;
		int value = 2456;

		varUnsafeBuckets.close();

		try {
			varUnsafeBuckets.deleteCount( bucket, value, 3 );
			Assert.fail();
		} catch ( NullPointerException ex ) {

		}
	}
}
