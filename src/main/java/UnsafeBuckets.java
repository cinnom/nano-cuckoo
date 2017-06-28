import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Created by rjones on 6/22/17.
 */
public class UnsafeBuckets {

	private final long maxCapacity = 0x4000000000000000L;
	private final int maxEntries;

	private final Unsafe unsafe;
	private long[] addresses;

	private final long capacity;
	private int entries;

	private final int shiftRightBits;

	public UnsafeBuckets(int entries, long capacity, int maxEntries) throws NoSuchFieldException, IllegalAccessException {

		Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
		singleoneInstanceField.setAccessible(true);
		unsafe = (Unsafe) singleoneInstanceField.get(null);

		long realCapacity = Long.highestOneBit( capacity );

		if(realCapacity != capacity && realCapacity != maxCapacity) {
			realCapacity <<= 1;
		}

		QuickLogger.log( "Real capacity: " + realCapacity );

		shiftRightBits = 64 - Long.bitCount(realCapacity - 1);

		addresses = new long[entries];
		for(int i = 0; i < entries; i++) {
			addresses[i] = unsafe.allocateMemory( realCapacity );
			unsafe.setMemory( addresses[i], realCapacity, (byte) 0 );
		}

		this.entries = entries;
		this.capacity = realCapacity;
		this.maxEntries = maxEntries;
	}

	public long getMemoryUsageBytes() {
		return capacity * entries;
	}

	public long getBucket(long hash) {

		return hash >>> shiftRightBits;
	}

	private long getCheckAddress(int entry, long bucket) {

		return addresses[entry] + bucket;
	}

	private boolean isValueSet(int entry, long bucket) {

		return unsafe.getByte( getCheckAddress( entry, bucket ) ) != 0;
	}

	public boolean expand() {

		if (entries >= maxEntries) {
			return false;
		}

		long[] newAddresses = new long[++entries];

		for(int i = 0; i < entries - 1; i++) {
			newAddresses[i] = addresses[i];
		}
		newAddresses[entries - 1] = unsafe.allocateMemory( capacity );
		unsafe.setMemory( newAddresses[entries - 1], capacity, (byte) 0 );
		/*for(int i = entries / 2; i < entries; i++) {
			newAddresses[i] = unsafe.allocateMemory( capacity );
			unsafe.setMemory( newAddresses[i], capacity, (byte) 0 );
		}*/

		addresses = newAddresses;

		QuickLogger.log( "EXPANDED" );

		return true;
	}

	public boolean contains(long bucket, byte value) {

		QuickLogger.log( "Contains Bucket: " + bucket );

		for(int entry = 0; entry < entries; entry++) {

			if(unsafe.getByte( getCheckAddress( entry, bucket ) ) == value) {
				return true;
			}
		}
		return false;
	}

	public boolean insert(long bucket, byte value, boolean noDuplicate) {

		QuickLogger.log( "Insert Bucket: " + bucket );

		for(int entry = 0; entry < entries; entry++) {

			byte currentValue = unsafe.getByte( getCheckAddress( entry, bucket ) );
			if( currentValue == 0 ) {
				putByte( entry, bucket, value );
				QuickLogger.log( "Empty slot found: " + bucket );
				return true;
			}
			else if (currentValue == value && noDuplicate) {
				QuickLogger.log( "Duplicate fingerprint found" );
				return true;
			}
		}

		return false;
	}

	public byte swap(int entry, long bucket, byte value) {

		QuickLogger.log( "Swap Bucket: " + bucket );

		byte retVal = getByte( entry, bucket );
		putByte( entry, bucket, value );
		return retVal;
	}

	private byte getByte(int entry, long bucket) {

		return unsafe.getByte( getCheckAddress( entry, bucket ) );
	}

	private void putByte(int entry, long bucket, byte value) {

		unsafe.putByte( getCheckAddress( entry, bucket ), value );
	}

	public int getEntries() {

		return entries;
	}

	public long getCapacity() {

		return capacity;
	}

	public void delete() {

		for(int i = 0; i < entries; i++) {
			unsafe.freeMemory( addresses[i] );
		}
	}
}
