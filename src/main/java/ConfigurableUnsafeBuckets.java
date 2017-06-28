import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Created by rjones on 6/22/17.
 */
public class ConfigurableUnsafeBuckets {

	private static final int BITS_PER_INT = 32;
	private static final int BITS_PER_BYTE = 8;

	private final long maxCapacity = 0x4000000000000000L;
	private final int maxEntries;

	private final Unsafe unsafe;
	private long[] addresses;

	private final long capacity;
	private final long capacityInts;
	private final long capacityBytes;
	private int entries;

	private final int shiftRightBits;

	private final int getClearMask;
	private final int putClearMask;

	private int fpBits;

	public ConfigurableUnsafeBuckets(int entries, long capacity, int maxEntries, int fpBits) throws NoSuchFieldException, IllegalAccessException {

		Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
		singleoneInstanceField.setAccessible(true);
		unsafe = (Unsafe) singleoneInstanceField.get(null);

		long realCapacity = Long.highestOneBit( capacity );

		if(realCapacity != capacity && realCapacity != maxCapacity) {
			realCapacity <<= 1;
		}

		QuickLogger.log( "Real capacity: " + realCapacity );

		shiftRightBits = 64 - Long.bitCount(realCapacity - 1);

		long capacityInts = (long) Math.ceil((double) realCapacity * (double) fpBits / (double) BITS_PER_INT);
		long capacityBytes = capacityInts * BITS_PER_BYTE;

		addresses = new long[entries];
		for(int i = 0; i < entries; i++) {
			addresses[i] = unsafe.allocateMemory( capacityBytes);
			unsafe.setMemory( addresses[i], capacityBytes, (byte) 0 );
		}

		getClearMask = -1 >>> (BITS_PER_INT - fpBits) << (BITS_PER_INT - fpBits);
		putClearMask = ~getClearMask;

		this.entries = entries;
		this.capacity = realCapacity;
		this.maxEntries = maxEntries;
		this.capacityInts = capacityInts;
		this.capacityBytes = capacityBytes;
		this.fpBits = fpBits;
	}

	public long getMemoryUsageBytes() {
		return capacityBytes * entries;
	}

	public long getBucket(long hash) {

		return hash >>> shiftRightBits;
	}

	private long getCheckAddress(int entry, long bucket) {

		return addresses[entry] + bucket;
	}

	private boolean isValueSet(int entry, long bucket) {

		return getValue( entry, bucket ) != 0;
	}

	public boolean expand() {

		if (entries >= maxEntries) {
			return false;
		}

		long[] newAddresses = new long[++entries];

		for(int i = 0; i < entries - 1; i++) {
			newAddresses[i] = addresses[i];
		}
		newAddresses[entries - 1] = unsafe.allocateMemory(capacityBytes);
		unsafe.setMemory( newAddresses[entries - 1], capacityBytes, (byte) 0 );
		/*for(int i = entries / 2; i < entries; i++) {
			newAddresses[i] = unsafe.allocateMemory( capacity );
			unsafe.setMemory( newAddresses[i], capacity, (byte) 0 );
		}*/

		addresses = newAddresses;

		QuickLogger.log( "EXPANDED" );

		return true;
	}

	public boolean contains(long bucket, int value) {

		QuickLogger.log( "Contains Bucket: " + bucket );

		for(int entry = 0; entry < entries; entry++) {

			if(getValue( entry, bucket ) == value) {
				return true;
			}
		}
		return false;
	}

	public boolean insert(long bucket, byte value, boolean noDuplicate) {

		QuickLogger.log( "Insert Bucket: " + bucket );

		for(int entry = 0; entry < entries; entry++) {

			int currentValue = getValue( entry, bucket );
			if( currentValue == 0 ) {
				putValue( entry, bucket, value );
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

	public int swap(int entry, long bucket, byte value) {

		QuickLogger.log( "Swap Bucket: " + bucket );

		int retVal = getValue( entry, bucket );
		putValue( entry, bucket, value );
		return retVal;
	}

	private int getValue(int entry, long bucket) {

		long bucketBits = bucket * fpBits;

		long bucketInt = bucketBits / BITS_PER_INT;
		long startBit = bucketBits % BITS_PER_INT;

		int targetInt1 = unsafe.getInt(bucketInt);

		int clearMask = getClearMask >>> startBit;

		return targetInt1 & clearMask;
	}

	private void putValue(int entry, long bucket, int value) {

		long bucketBits = bucket * fpBits;

		long bucketInt = bucketBits / BITS_PER_INT;
		long startBit = bucketBits % BITS_PER_INT;

		int targetInt1 = unsafe.getInt(bucketInt);

		int clearMask = putClearMask >>> startBit;

		targetInt1 = (targetInt1 & clearMask) | value;

		unsafe.putInt( bucketInt, targetInt1 );
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
