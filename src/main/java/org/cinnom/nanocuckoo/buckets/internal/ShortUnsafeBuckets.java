package org.cinnom.nanocuckoo.buckets.internal;

/**
 * Created by rjones on 6/22/17.
 */
public class ShortUnsafeBuckets extends UnsafeBuckets {

	private static final int FP_BITS = 16;
	private static final int BYTE_SHIFT = 1;

	public ShortUnsafeBuckets(int entries, long capacity, int maxEntries ) throws NoSuchFieldException, IllegalAccessException {

		super (entries, capacity, maxEntries, FP_BITS );
	}

	protected int getValue(int entry, long bucket) {

		return unsafe.getShort( addresses[entry] + (bucket << BYTE_SHIFT) ) & 0x0000ffff;
	}

	protected void putValue(int entry, long bucket, int value) {

		unsafe.putShort( addresses[entry] + (bucket << BYTE_SHIFT), (short) value );
	}

}
