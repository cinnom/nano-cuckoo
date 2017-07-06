package org.cinnom.nanocuckoo;

/**
 * UnsafeBuckets type to be used when fingerprints are exactly 32 bits.
 */
final class IntUnsafeBuckets extends UnsafeBuckets {

    private static final int FP_BITS = 32;
    private static final int BYTE_SHIFT = 2;

    IntUnsafeBuckets(int entries, long capacity, int maxEntries, boolean countingDisabled) throws NoSuchFieldException, IllegalAccessException {

        super(entries, capacity, maxEntries, FP_BITS, countingDisabled);
    }

    @Override
    int swap(int entry, long bucket, int value) {

        return unsafe.getAndSetInt(null, addresses[entry] + (bucket << BYTE_SHIFT), value);
    }

    @Override
    int getValue(int entry, long bucket) {

        return unsafe.getIntVolatile(null, addresses[entry] + (bucket << BYTE_SHIFT));
    }

    @Override
    void putValue(int entry, long bucket, int value) {

        unsafe.putIntVolatile(null, addresses[entry] + (bucket << BYTE_SHIFT), value);
    }

}
