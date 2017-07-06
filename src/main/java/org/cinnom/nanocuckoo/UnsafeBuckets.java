package org.cinnom.nanocuckoo;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import sun.misc.Unsafe;

/**
 * Internal bucket implementation that uses sun.misc.Unsafe for native memory allocation.
 */
abstract class UnsafeBuckets {

	private static final int BITS_PER_LONG = 64;

	private static final int DIV_8 = 3;
	private static final long MOD_8_MASK = 0x0000000000000007;

	private static final long MAX_CAPACITY = 0x0100000000000000L;

	private final long capacity;
	private final long capacityBytes;
	private final boolean countingDisabled;
	private int entries;
	private int entryMask;

	private final int bucketBits;

	private final AtomicLong insertedCount = new AtomicLong();

	final Unsafe unsafe;
	long[] addresses;
	int fpBits;

	UnsafeBuckets( int entries, long bucketCount, int fpBits, boolean countingDisabled )
			throws NoSuchFieldException, IllegalAccessException {

		Field singleoneInstanceField = Unsafe.class.getDeclaredField( "theUnsafe" );
		singleoneInstanceField.setAccessible( true );
		unsafe = (Unsafe) singleoneInstanceField.get( null );

		long realCapacity = Math.min( Long.highestOneBit( bucketCount ), MAX_CAPACITY );

		if ( realCapacity != bucketCount && realCapacity < MAX_CAPACITY ) {
			realCapacity <<= 1;
		}

		bucketBits = BITS_PER_LONG - Long.bitCount( realCapacity - 1 );

		this.entries = entries;
		entryMask = this.entries - 1;

		long capacityBytes = ( realCapacity >>> DIV_8 ) * fpBits;
		if ( ( capacityBytes & MOD_8_MASK ) > 0 ) {
			capacityBytes++;
		}

		addresses = new long[this.entries];
		for ( int i = 0; i < this.entries; i++ ) {
			addresses[i] = unsafe.allocateMemory( capacityBytes );
			unsafe.setMemory( addresses[i], capacityBytes, (byte) 0 );
		}

		this.capacity = realCapacity;
		this.capacityBytes = capacityBytes;
		this.fpBits = fpBits;
		this.countingDisabled = countingDisabled;
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

		insertedCount.decrementAndGet();
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

	int deleteAll( long bucket, int value ) {

		int deletedCount = 0;
		for ( int entry = 0; entry < entries; entry++ ) {

			if ( getValue( entry, bucket ) == value ) {
				putValue( entry, bucket, 0 );
				deletedCount++;
			}
		}
		insertedCount.addAndGet( -deletedCount );
		return deletedCount;
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

	int getEntriesPerBucket() {

		return entries;
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
	}

	abstract int getValue( int entry, long bucket );

	abstract void putValue( int entry, long bucket, int value );

}
