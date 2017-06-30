package org.cinnom.nanocuckoo;

/**
 * Created by rjones on 6/22/17.
 */
final class IntUnsafeBuckets extends UnsafeBuckets {

	private static final int FP_BITS = 32;
	private static final int BYTE_SHIFT = 2;

	public IntUnsafeBuckets(int entries, long capacity, int maxEntries ) throws NoSuchFieldException, IllegalAccessException {

		super (entries, capacity, maxEntries, FP_BITS );
	}

	protected int getValue(int entry, long bucket) {

		return unsafe.getInt( addresses[entry] + (bucket << BYTE_SHIFT) );
	}

	protected void putValue(int entry, long bucket, int value) {

		unsafe.putInt( addresses[entry] + (bucket << BYTE_SHIFT), value );
	}

}
