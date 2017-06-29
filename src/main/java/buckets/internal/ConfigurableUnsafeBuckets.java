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

	@Override
	public int swap( int entry, long bucket, int value ) {

		return swapValue(entry, bucket, value );
	}

	// less ops than get/put
	private int swapValue(int entry, long bucket, int value) {

		long bucketBits = bucket * fpBits;

		long bucketByte = addresses[entry] + (bucketBits >>> DIV_32 << MULTI_4 );
		int startBit = ((int) (bucketBits)) & MOD_32_MASK;

		int clearMask = initClearMask >>> startBit;

		int shift = BITS_PER_INT - (startBit + fpBits);

		int getInt = unsafe.getInt( bucketByte );
		int putInt = getInt;

		getInt &= clearMask;

		int putValue = value;
		startBit = 0;
		if(shift > 0) {
			getInt >>>= shift;
			putValue = value << shift;
		}
		else if (shift < 0) {
			startBit = -shift;
			getInt <<= startBit;
			putValue = value >>> startBit;
		}

		putInt = ( putInt & (~clearMask) ) | putValue;

		unsafe.putInt( bucketByte, putInt );

		if(startBit > 0) {

			bucketByte += BYTES_PER_INT;

			shift = BITS_PER_INT - startBit;

			putInt = unsafe.getInt( bucketByte );

			getInt |= putInt >>> shift;

			clearMask = -1 >>> startBit;

			putValue = value << shift;

			putInt = ( putInt & clearMask ) | putValue;

			unsafe.putInt( bucketByte, putInt );
		}

		return getInt;
	}

	public int getValue( int entry, long bucket ) {

		long bucketBits = bucket * fpBits;

		long bucketByte = addresses[entry] + (bucketBits >>> DIV_32 << MULTI_4 );
		int startBit = ((int) (bucketBits)) & MOD_32_MASK;

		int clearMask = initClearMask >>> startBit;

		int shift = BITS_PER_INT - (startBit + fpBits);

		int getInt = unsafe.getInt( bucketByte );

		getInt &= clearMask;

		startBit = 0;

		getInt = shift > 0 ? getInt >>> shift : getInt;
		getInt = shift < 0 ? getInt << (startBit = -shift) : getInt;

		if(startBit > 0) {

			int getInt2 = unsafe.getInt( bucketByte + BYTES_PER_INT );

			getInt2 >>>= (BITS_PER_INT - startBit);

			getInt |= getInt2;
		}

		return getInt;
	}

	public void putValue( int entry, long bucket, int value ) {

		long bucketBits = bucket * fpBits;

		long bucketByte = addresses[entry] + (bucketBits >>> DIV_32 << MULTI_4 );
		int startBit = ((int) (bucketBits)) & MOD_32_MASK;

		int clearMask = ~( initClearMask >>> startBit);

		int leftShift = BITS_PER_INT - (startBit + fpBits);

		startBit = 0;

		int putValue = leftShift > 0 ? value << leftShift : value;
		putValue = leftShift < 0 ? value >>> (startBit = -leftShift) : putValue;

		int putInt = unsafe.getInt( bucketByte );

		putInt = ( putInt & clearMask ) | putValue;

		unsafe.putInt( bucketByte, putInt );

		if ( startBit > 0 ) {

			clearMask = -1 >>> startBit;

			putValue = value << (BITS_PER_INT - startBit);

			bucketByte += BYTES_PER_INT;

			putInt = unsafe.getInt( bucketByte );

			putInt = ( putInt & clearMask ) | putValue;

			unsafe.putInt( bucketByte, putInt );
		}

	}


}
