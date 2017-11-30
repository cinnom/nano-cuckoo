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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import sun.misc.Unsafe;

/**
 * Internal bucket implementation that uses sun.misc.Unsafe for native memory allocation.
 */
abstract class UnsafeBuckets {

	private static final int BITS_PER_LONG = 64;

	private static final int DIV_8 = 3;

	private static final long MAX_CAPACITY = 0x1000000000000000L;
	private static final long MIN_CAPACITY = 0x0000000000000008L;

	private final long capacity;
	private final boolean countingDisabled;
	private int entries;
	int fpBits;

	private int entryMask;
	private final long capacityBytes;
	private final int bucketBits;

	private final AtomicLong insertedCount;

	Unsafe unsafe;
	long[] addresses;

	UnsafeBuckets( int entries, long bucketCount, int fpBits, boolean countingDisabled, long initialCount ) {

		unsafe = getUnsafe();

		long realCapacity = Math.max( Math.min( Long.highestOneBit( bucketCount ), MAX_CAPACITY ), MIN_CAPACITY );

		if ( realCapacity < bucketCount && realCapacity < MAX_CAPACITY ) {
			realCapacity <<= 1;
		}

		this.entries = entries;
		this.capacity = realCapacity;
		this.fpBits = fpBits;
		this.countingDisabled = countingDisabled;
		this.insertedCount = new AtomicLong( initialCount );

		this.entryMask = this.entries - 1;
		this.capacityBytes = ( capacity >>> DIV_8 ) * fpBits;
		this.bucketBits = BITS_PER_LONG - Long.bitCount( capacity - 1 );

		allocateMemory();
	}

	static UnsafeBuckets createBuckets( final int fpBits, final int entriesPerBucket, final long bucketCount,
			final boolean countingDisabled, final long initialCount ) {

		switch ( fpBits ) {
			case ByteUnsafeBuckets.FP_BITS:
				return new ByteUnsafeBuckets( entriesPerBucket, bucketCount, countingDisabled, initialCount );
			case ShortUnsafeBuckets.FP_BITS:
				return new ShortUnsafeBuckets( entriesPerBucket, bucketCount, countingDisabled, initialCount );
			case IntUnsafeBuckets.FP_BITS:
				return new IntUnsafeBuckets( entriesPerBucket, bucketCount, countingDisabled, initialCount );
			default:
				return new VariableUnsafeBuckets( entriesPerBucket, bucketCount, fpBits, countingDisabled,
						initialCount );
		}
	}

	int getEntryMask() {

		return entryMask;
	}

	long getMemoryUsageBytes() {

		return capacityBytes * entries;
	}

	long getBucket( long hash ) {

		return hash >>> bucketBits;
	}

	long getInsertedCount() {

		return insertedCount.get();
	}

	void incrementInsertedCount() {

		insertedCount.incrementAndGet();
	}

	void decrementInsertedCount() {

		insertedCount.decrementAndGet();
	}

	long getBucketCount() {

		return capacity;
	}

	int getEntriesPerBucket() {

		return entries;
	}

	long getTotalCapacity() {

		return capacity * entries;
	}

	boolean isCountingDisabled() {

		return countingDisabled;
	}

	void expand() {

		entries *= 2;
		entryMask = entries - 1;

		long[] newAddresses = new long[entries];

		System.arraycopy( addresses, 0, newAddresses, 0, entries / 2 );
		for ( int i = entries / 2; i < entries; i++ ) {
			newAddresses[i] = unsafe.allocateMemory( capacityBytes );
			unsafe.setMemory( newAddresses[i], capacityBytes, (byte) 0 );
		}

		addresses = newAddresses;
	}

	boolean contains( long bucket, int value ) {

		for ( int entry = 0; entry < entries; entry++ ) {

			if ( getValue( entry, bucket ) == value ) {
				return true;
			}
		}
		return false;
	}

	int count( long bucket, int value ) {

		int counted = 0;
		for ( int entry = 0; entry < entries; entry++ ) {

			if ( getValue( entry, bucket ) == value ) {
				counted++;
			}
		}
		return counted;
	}

	boolean delete( long bucket, int value ) {

		for ( int entry = 0; entry < entries; entry++ ) {

			if ( getValue( entry, bucket ) == value ) {
				putValue( entry, bucket, 0 );
				insertedCount.decrementAndGet();
				return true;
			}
		}
		return false;
	}

	int deleteCount( long bucket, int value, int count ) {

		int deletedCount = 0;
		for ( int entry = 0; entry < entries; entry++ ) {

			if ( getValue( entry, bucket ) == value ) {
				putValue( entry, bucket, 0 );
				if ( ++deletedCount >= count ) {
					break;
				}
			}
		}
		insertedCount.addAndGet( -deletedCount );
		return deletedCount;
	}

	boolean insert( long bucket, int value ) {

		for ( int entry = 0; entry < entries; entry++ ) {

			int currentValue = getValue( entry, bucket );
			if ( currentValue == 0 ) {
				putValue( entry, bucket, value );
				insertedCount.incrementAndGet();
				return true;
			} else if ( currentValue == value && countingDisabled ) {
				// Do not increment count
				return true;
			}
		}

		return false;
	}

	int swap( int entry, long bucket, int value ) {

		int retVal = getValue( entry, bucket );
		putValue( entry, bucket, value );
		return retVal;
	}

	void close() {

		for ( int i = 0; i < entries; i++ ) {
			unsafe.freeMemory( addresses[i] );
		}

		// Set unsafe to null; NPE is better than a segfault
		unsafe = null;
	}

	abstract int getValue( int entry, long bucket );

	abstract void putValue( int entry, long bucket, int value );

	void writeMemory( OutputStream outputStream ) throws IOException {

		for ( int entry = 0; entry < entries; entry++ ) {
			for ( long memoryByte = 0; memoryByte < capacityBytes; memoryByte++ ) {
				int value = unsafe.getByteVolatile( null, addresses[entry] + memoryByte ) & 0x000000FF;
				outputStream.write( value );
			}
		}
	}

	void readMemory( InputStream inputStream ) throws IOException {

		for ( int entry = 0; entry < entries; entry++ ) {
			for ( long memoryByte = 0; memoryByte < capacityBytes; memoryByte++ ) {
				int value = inputStream.read();
				unsafe.putByteVolatile( null, addresses[entry] + memoryByte, (byte) value );
			}
		}
	}

	private void allocateMemory() {

		addresses = new long[this.entries];
		for ( int i = 0; i < this.entries; i++ ) {
			addresses[i] = unsafe.allocateMemory( capacityBytes );
			unsafe.setMemory( addresses[i], capacityBytes, (byte) 0 );
		}
	}

	private Unsafe getUnsafe() {

		try {
			Field singleoneInstanceField = Unsafe.class.getDeclaredField( "theUnsafe" );
			singleoneInstanceField.setAccessible( true );
			return (Unsafe) singleoneInstanceField.get( null );
		} catch ( IllegalAccessException | NoSuchFieldException e ) {
			throw new RuntimeException( "Failed to obtain Unsafe", e );
		}
	}
}
