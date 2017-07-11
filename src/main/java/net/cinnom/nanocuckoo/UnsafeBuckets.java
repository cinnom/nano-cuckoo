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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import sun.misc.Unsafe;

/**
 * Internal bucket implementation that uses sun.misc.Unsafe for native memory allocation.
 */
abstract class UnsafeBuckets implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final int BITS_PER_LONG = 64;

	private static final int DIV_8 = 3;

	private static final long MAX_CAPACITY = 0x1000000000000000L;
	private static final long MIN_CAPACITY = 0x0000000000000008L;

	private final long capacity;
	private final long capacityBytes;
	private final boolean countingDisabled;
	private int entries;
	private int entryMask;

	private final int bucketBits;

	private final AtomicLong insertedCount = new AtomicLong();

	transient Unsafe unsafe;
	transient long[] addresses;
	int fpBits;

	UnsafeBuckets( int entries, long bucketCount, int fpBits, boolean countingDisabled ) {

		unsafe = getUnsafe();

		long realCapacity = Math.max( Math.min( Long.highestOneBit( bucketCount ), MAX_CAPACITY ), MIN_CAPACITY );

		if ( realCapacity < bucketCount && realCapacity < MAX_CAPACITY ) {
			realCapacity <<= 1;
		}

		bucketBits = BITS_PER_LONG - Long.bitCount( realCapacity - 1 );

		this.entries = entries;
		entryMask = this.entries - 1;

		long capacityBytes = ( realCapacity >>> DIV_8 ) * fpBits;

		this.capacity = realCapacity;
		this.capacityBytes = capacityBytes;
		this.fpBits = fpBits;
		this.countingDisabled = countingDisabled;

		allocateMemory();
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

	long getBucketCount() {

		return capacity;
	}

	long getCapacity() {

		return capacity * entries;
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

	private void writeMemory( OutputStream outputStream ) throws IOException {

		for ( int entry = 0; entry < entries; entry++ ) {
			for ( long memoryByte = 0; memoryByte < capacityBytes; memoryByte++ ) {
				int value = unsafe.getByteVolatile( null, addresses[entry] + memoryByte ) & 0x000000FF;
				outputStream.write( value );
			}
		}
	}

	private void readMemory( InputStream inputStream ) throws IOException {

		for ( int entry = 0; entry < entries; entry++ ) {
			for ( long memoryByte = 0; memoryByte < capacityBytes; memoryByte++ ) {
				int value = inputStream.read();
				unsafe.putByteVolatile( null, addresses[entry] + memoryByte, (byte) value );
			}
		}
	}

	private void writeObject( ObjectOutputStream out ) throws IOException {

		out.writeLong( capacity );
		out.writeLong( capacityBytes );
		out.writeBoolean( countingDisabled );
		out.writeInt( entries );
		out.writeInt( entryMask );
		out.writeInt( bucketBits );
		out.writeLong( insertedCount.get() );
		out.writeInt( fpBits );
		writeMemory( out );
		out.flush();
	}

	private void readObject( ObjectInputStream in ) throws IOException, ClassNotFoundException {

		try {
			unsafe = getUnsafe();

			long capacityOffset = unsafe.objectFieldOffset( UnsafeBuckets.class.getDeclaredField( "capacity" ) );
			long capacityBytesOffset = unsafe
					.objectFieldOffset( UnsafeBuckets.class.getDeclaredField( "capacityBytes" ) );
			long countingDisabledOffset = unsafe
					.objectFieldOffset( UnsafeBuckets.class.getDeclaredField( "countingDisabled" ) );
			long bucketBitsOffset = unsafe.objectFieldOffset( UnsafeBuckets.class.getDeclaredField( "bucketBits" ) );
			long insertedCountOffset = unsafe
					.objectFieldOffset( UnsafeBuckets.class.getDeclaredField( "insertedCount" ) );

			// We need Unsafe anyways, may as well abuse it to write to final variables
			unsafe.putLong( this, capacityOffset, in.readLong() );
			unsafe.putLong( this, capacityBytesOffset, in.readLong() );
			unsafe.putBoolean( this, countingDisabledOffset, in.readBoolean() );
			entries = in.readInt();
			entryMask = in.readInt();
			unsafe.putInt( this, bucketBitsOffset, in.readInt() );
			unsafe.putObject( this, insertedCountOffset, new AtomicLong( in.readLong() ) );
			fpBits = in.readInt();
			allocateMemory();
			readMemory( in );
		} catch ( NoSuchFieldException e ) {
			throw new RuntimeException( "Couldn't locate field during deserialization", e );
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
