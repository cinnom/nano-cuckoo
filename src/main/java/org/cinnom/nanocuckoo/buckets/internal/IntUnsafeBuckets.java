package org.cinnom.nanocuckoo.buckets.internal;

/**
 * Created by rjones on 6/22/17.
 */
public class IntUnsafeBuckets extends UnsafeBuckets {

	private static final int FP_BITS = 32;
	private static final int BYTE_SHIFT = 2;

	public IntUnsafeBuckets(int entries, long capacity, int maxEntries, boolean allowUpsize ) throws NoSuchFieldException, IllegalAccessException {

		super (entries, capacity, maxEntries, FP_BITS, allowUpsize );
	}

	protected int getValue(int entry, long bucket) {

		return unsafe.getInt( addresses[entry] + (bucket << BYTE_SHIFT) );
	}

	protected void putValue(int entry, long bucket, int value) {

		unsafe.putInt( addresses[entry] + (bucket << BYTE_SHIFT), value );
	}

}
