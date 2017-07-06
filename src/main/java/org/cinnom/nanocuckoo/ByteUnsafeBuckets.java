package org.cinnom.nanocuckoo;

/**
 * UnsafeBuckets type to be used when fingerprints are exactly 8 bits. Makes calls to Unsafe Byte methods.
 */
final class ByteUnsafeBuckets extends UnsafeBuckets {

	private static final int FP_BITS = 8;

	ByteUnsafeBuckets( int entries, long bucketCount, boolean countingDisabled )
			throws NoSuchFieldException, IllegalAccessException {

		super( entries, bucketCount, FP_BITS, countingDisabled );
	}

	@Override
	int getValue( int entry, long bucket ) {

		return unsafe.getByteVolatile( null, addresses[entry] + bucket ) & 0x000000FF;
	}

	@Override
	void putValue( int entry, long bucket, int value ) {

		unsafe.putByteVolatile( null, addresses[entry] + bucket, (byte) value );
	}
}
