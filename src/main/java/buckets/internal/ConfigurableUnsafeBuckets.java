package buckets.internal;

/**
 * Created by rjones on 6/22/17.
 */
public class ConfigurableUnsafeBuckets extends UnsafeBuckets {

	private static final int BITS_PER_INT = 32;
	private static final int DIV_32 = 5;
	private static final int BYTES_PER_INT = 4;
	private static final int MULTI_4 = 2;
	private static final int MOD_32_MASK = 0x0000001F;

	private final int initClearMask;


	public ConfigurableUnsafeBuckets( int entries, long capacity, int maxEntries, int fpBits )
			throws NoSuchFieldException, IllegalAccessException {

		super( entries, capacity, maxEntries, fpBits );

		int shift = BITS_PER_INT - fpBits;
		initClearMask = (-1 >>> shift) << shift;
	}


	// TODO possibly make special get/put for swapping?

	public int getValue( int entry, long bucket ) {

		long bucketBits = bucket * fpBits;

		long bucketByte = addresses[entry] + (bucketBits >>> DIV_32 << MULTI_4 );
		int startBit = ((int) (bucketBits)) & MOD_32_MASK;

		int clearMask = initClearMask >>> startBit;

		int rightShift = BITS_PER_INT - (startBit + fpBits);

		int targetInt = unsafe.getInt( bucketByte );

		targetInt &= clearMask;

		startBit = 0;

		targetInt = rightShift > 0 ? targetInt >>> rightShift : targetInt;
		targetInt = rightShift < 0 ? targetInt << (startBit = -rightShift) : targetInt;

		if(startBit > 0) {

			int targetInt2 = unsafe.getInt( bucketByte + BYTES_PER_INT );

			targetInt2 >>>= (BITS_PER_INT - startBit);

			targetInt |= targetInt2;
		}

		return targetInt;
	}

	public void putValue( int entry, long bucket, int value ) {

		long bucketBits = bucket * fpBits;

		long bucketByte = addresses[entry] + (bucketBits >>> DIV_32 << MULTI_4 );
		int startBit = ((int) (bucketBits)) & MOD_32_MASK;

		int clearMask = ~( initClearMask >>> startBit);

		int leftShift = BITS_PER_INT - (startBit + fpBits);

		startBit = 0;

		int writeValue = leftShift > 0 ? value << leftShift : value;
		writeValue = leftShift < 0 ? value >>> (startBit = -leftShift) : writeValue;

		int targetInt = unsafe.getInt( bucketByte );

		targetInt = ( targetInt & clearMask ) | writeValue;

		unsafe.putInt( bucketByte, targetInt );

		if ( startBit > 0 ) {

			clearMask = -1 >>> startBit;

			writeValue = value << (BITS_PER_INT - startBit);

			bucketByte += BYTES_PER_INT;

			targetInt = unsafe.getInt( bucketByte );

			targetInt = ( targetInt & clearMask ) | writeValue;

			unsafe.putInt( bucketByte, targetInt );
		}

	}


}
