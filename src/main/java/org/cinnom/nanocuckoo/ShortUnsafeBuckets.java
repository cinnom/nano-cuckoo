package org.cinnom.nanocuckoo;

/**
 * UnsafeBuckets type to be used when fingerprints are exactly 16 bits. Makes calls to Unsafe Short methods.
 */
final class ShortUnsafeBuckets extends UnsafeBuckets {

	private static final int FP_BITS = 16;
	private static final int BYTE_SHIFT = 1;

	ShortUnsafeBuckets( int entries, long bucketCount, boolean countingDisabled )
			throws NoSuchFieldException, IllegalAccessException {

		super( entries, bucketCount, FP_BITS, countingDisabled );
	}

	@Override
	int getValue( int entry, long bucket ) {

		return unsafe.getShortVolatile( null, addresses[entry] + ( bucket << BYTE_SHIFT ) ) & 0x0000FFFF;
	}

	@Override
	void putValue( int entry, long bucket, int value ) {

		unsafe.putShortVolatile( null, addresses[entry] + ( bucket << BYTE_SHIFT ), (short) value );
	}

}
