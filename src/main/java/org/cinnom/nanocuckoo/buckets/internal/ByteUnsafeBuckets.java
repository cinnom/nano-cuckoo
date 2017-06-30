package org.cinnom.nanocuckoo.buckets.internal;

/**
 * Created by rjones on 6/22/17.
 */
public class ByteUnsafeBuckets extends UnsafeBuckets {

	private static final int FP_BITS = 8;

	public ByteUnsafeBuckets(int entries, long capacity, int maxEntries ) throws NoSuchFieldException, IllegalAccessException {

		super (entries, capacity, maxEntries, FP_BITS );
	}

	protected int getValue(int entry, long bucket) {

		return unsafe.getByte( addresses[entry] + bucket ) & 0x000000ff;
	}

	protected void putValue(int entry, long bucket, int value) {

		unsafe.putByte( addresses[entry] + bucket, (byte) value );
	}
}
