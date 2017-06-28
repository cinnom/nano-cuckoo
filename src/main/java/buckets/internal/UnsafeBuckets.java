package buckets.internal;

import buckets.Buckets;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Created by rjones on 6/28/17.
 */
public abstract class UnsafeBuckets implements Buckets {

	private static final int DIV_8 = 3;
	private static final long MOD_8_MASK = 0x0000000000000007;

	private final long maxCapacity = 0x0100000000000000L;
	private final int maxEntries;

	private final long capacity;
	private final long capacityBytes;
	private int entries;

	private final int shiftRightBits;

	protected final Unsafe unsafe;
	protected long[] addresses;

	protected int fpBits;

	public UnsafeBuckets( int entries, long capacity, int maxEntries, int fpBits )
			throws NoSuchFieldException, IllegalAccessException {

		Field singleoneInstanceField = Unsafe.class.getDeclaredField( "theUnsafe" );
		singleoneInstanceField.setAccessible( true );
		unsafe = (Unsafe) singleoneInstanceField.get( null );

		long realCapacity = Math.min(Long.highestOneBit( capacity ), maxCapacity);

		if ( realCapacity != capacity && realCapacity < maxCapacity ) {
			realCapacity <<= 1;
		}

		shiftRightBits = 64 - Long.bitCount( realCapacity - 1 );

		long capacityBytes = (realCapacity >>> DIV_8) * fpBits;

		if((realCapacity & MOD_8_MASK) > 0) {
			capacityBytes++;
		}

		addresses = new long[entries];
		for ( int i = 0; i < entries; i++ ) {
			addresses[i] = unsafe.allocateMemory( capacityBytes );
			unsafe.setMemory( addresses[i], capacityBytes, (byte) 0 );
		}

		this.entries = entries;
		this.capacity = realCapacity;
		this.maxEntries = maxEntries;
		this.capacityBytes = capacityBytes;
		this.fpBits = fpBits;
	}

	public long getMemoryUsageBytes() {

		return capacityBytes * entries;
	}

	public long getBucket( long hash ) {

		return hash >>> shiftRightBits;
	}

	public boolean expand() {

		if ( entries >= maxEntries ) {
			return false;
		}

		long[] newAddresses = new long[++entries];

		for ( int i = 0; i < entries - 1; i++ ) {
			newAddresses[i] = addresses[i];
		}
		newAddresses[entries - 1] = unsafe.allocateMemory( capacityBytes );
		unsafe.setMemory( newAddresses[entries - 1], capacityBytes, (byte) 0 );

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
