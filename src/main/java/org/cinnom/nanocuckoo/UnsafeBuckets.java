package org.cinnom.nanocuckoo;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by rjones on 6/28/17.
 */
abstract class UnsafeBuckets {

	private static final int DIV_8 = 3;
	private static final long MOD_8_MASK = 0x0000000000000007;

	private final long maxCapacity = 0x0100000000000000L;
	private final int maxEntries;

	private final long capacity;
	private final long capacityBytes;
	private int entries;
	private int entryMask;

	private final int bucketBits;

	private int duplicates = 0;

	final Unsafe unsafe;
	long[] addresses;
	int fpBits;


	public UnsafeBuckets( int entries, long capacity, int maxEntries, int fpBits )
			throws NoSuchFieldException, IllegalAccessException {

		Field singleoneInstanceField = Unsafe.class.getDeclaredField( "theUnsafe" );
		singleoneInstanceField.setAccessible( true );
		unsafe = (Unsafe) singleoneInstanceField.get( null );

		long realCapacity = Math.min( Long.highestOneBit( capacity ), maxCapacity );

		if ( realCapacity != capacity && realCapacity < maxCapacity ) {
			realCapacity <<= 1;
		}

		bucketBits = 64 - Long.bitCount( realCapacity - 1 );

		this.entries = entries;
		entryMask = this.entries - 1;

		long capacityBytes = (realCapacity >>> DIV_8) * fpBits + 4;

		addresses = new long[this.entries];
		for ( int i = 0; i < this.entries; i++ ) {
			addresses[i] = unsafe.allocateMemory( capacityBytes );
			unsafe.setMemory( addresses[i], capacityBytes, (byte) 0 );
		}

		this.capacity = realCapacity;
		this.maxEntries = maxEntries;
		this.capacityBytes = capacityBytes;
		this.fpBits = fpBits;
	}

	public int getEntryMask() {

		return entryMask;
	}

	public long getMemoryUsageBytes() {

		return capacityBytes * entries;
	}

	public long getBucket( long hash ) {

		return hash >>> bucketBits;
	}

	public int getDuplicates() {
		return duplicates;
	}

	public boolean expand() {

		if ( entries >= maxEntries ) {
			return false;
		}

		entries *= 2;
		entryMask = entries - 1;

		long[] newAddresses = new long[entries];

		for ( int i = 0; i < entries / 2; i++ ) {
			newAddresses[i] = addresses[i];
		}
		for ( int i = entries / 2; i < entries; i++ ) {
			newAddresses[i] = unsafe.allocateMemory( capacityBytes );
			unsafe.setMemory( newAddresses[i], capacityBytes, (byte) 0 );
		}

		addresses = newAddresses;

		return true;
	}

	public boolean contains( long bucket, int value ) {

		for ( int entry = 0; entry < entries; entry++ ) {

			if ( getValue( entry, bucket ) == value ) {
				return true;
			}
		}
		return false;
	}

	public boolean insert( long bucket, int value, boolean noDuplicate ) {

		for ( int entry = 0; entry < entries; entry++ ) {

			int currentValue = getValue( entry, bucket );
			if ( currentValue == 0 ) {
				putValue( entry, bucket, value );
				return true;
			} else if ( currentValue == value && noDuplicate ) {
				duplicates++;
				return true;
			}
		}

		return false;
	}

	public int swap( int entry, long bucket, int value ) {

		int retVal = getValue( entry, bucket );
		putValue( entry, bucket, value );
		return retVal;
	}

	public int getEntries() {

		return entries;
	}

	public long getCapacity() {

		return capacity;
	}

	public void delete() {

		for ( int i = 0; i < entries; i++ ) {
			unsafe.freeMemory( addresses[i] );
		}
	}

	protected abstract int getValue( int entry, long bucket );

	protected abstract void putValue( int entry, long bucket, int value );

}
