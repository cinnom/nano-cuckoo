import buckets.*;
import buckets.internal.ByteUnsafeBuckets;
import buckets.internal.ConfigurableUnsafeBuckets;
import buckets.internal.IntUnsafeBuckets;
import buckets.internal.ShortUnsafeBuckets;
import hash.BucketHasher;
import hash.DumbHasher;
import hash.FingerprintHasher;
import hash.XXHasher;
import random.Randomizer;

import java.nio.charset.StandardCharsets;

/**
 * Created by rjones on 6/22/17.
 */
public class CuckooFilter {

	private static final int MAX_NUM_KICKS = 128;

	private static final int BITS_PER_INT = 32;
	private static final int BITS_PER_LONG = 64;

	private static int seed = 0x48f7e28a;

	private final Buckets buckets;
	private final BucketHasher bucketHasher;
	private final FingerprintHasher fpHasher;

	private final Randomizer random;

	private boolean allowExpand;

	private long lastContainsHash;
	private int bootedFingerprint;
	private long bootedBucket1;
	private long bootedBucket2;

	boolean insertAllowed = true;

	private final int fpBits;
	private final int fpPerLong;
	private final int fpMask;

	public CuckooFilter( int entryBits, long capacity, boolean allowExpand, int maxEntries, int fpBits ) throws NoSuchFieldException, IllegalAccessException {

		switch (fpBits) {
			case 8:
				buckets = new ByteUnsafeBuckets( entryBits, capacity, maxEntries );
				break;
			case 16:
				buckets = new ShortUnsafeBuckets( entryBits, capacity, maxEntries );
				break;
			case 32:
				buckets = new IntUnsafeBuckets( entryBits, capacity, maxEntries );
				break;
			default:
				buckets = new ConfigurableUnsafeBuckets( entryBits, capacity, maxEntries, fpBits );
				break;
		}

		XXHasher xxHasher = new XXHasher( seed );
		DumbHasher dumbHasher = new DumbHasher( fpBits, seed );

		bucketHasher = xxHasher;
		random = dumbHasher;

		this.allowExpand = allowExpand;
		this.fpBits = fpBits;

		fpPerLong = BITS_PER_LONG / fpBits;

		int shift = BITS_PER_INT - fpBits;
		fpMask = -1 >>> shift;

		if(fpBits <= 16) {
			fpHasher = dumbHasher;
		}
		else {
			fpHasher = xxHasher;
		}
	}

	public boolean insert( String value ) {

		final byte[] data = value.getBytes( StandardCharsets.UTF_8 ); // optimize

		return insert( data );
	}

	public boolean insert( byte[] data ) {

		if(!insertAllowed) {
			return false;
		}

		long hash = bucketHasher.getHash( data );

		long bucket1 = buckets.getBucket( hash );

		int fingerprint = fingerprintFromLong( hash );

		insertAllowed = insertFingerprint( fingerprint, bucket1 );

		return insertAllowed;
	}

	private int fingerprintFromLong(long hash) {

		for(int i = 0; i < fpPerLong; i++) {

			int tempFp = ((int) hash) & fpMask;
			if(tempFp != 0) {
				return tempFp;
			}
			hash >>>= fpBits;
		}

		return 1;
	}

	private boolean insertFingerprint(int fingerprint, long bucket1) {

		if ( buckets.insert( bucket1, fingerprint, true ) ) {
			return true;
		}

		long fingerprintHash = fpHasher.getHash( fingerprint );

		//System.out.println(fingerprintHash);

		long bucket2 = bucket1 ^ buckets.getBucket( fingerprintHash );

		if ( buckets.insert( bucket2, fingerprint, true ) ) {
			return true;
		}

		// long bucket = random.nextBoolean() ? bucket1 : bucket2;
		long bucket = bucket2;

		for ( int n = 0; n < MAX_NUM_KICKS; n++ ) {

			int entrySwap = random.nextInt() & buckets.getEntryMask();
			fingerprint = buckets.swap( entrySwap, bucket, fingerprint );
			bucket = bucket ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );
			if ( buckets.insert( bucket, fingerprint, true ) ) {
				return true;
			}
		}

		if(allowExpand && buckets.expand()) {
			return insertFingerprint( fingerprint, bucket );
		}

		bootedFingerprint = fingerprint;
		bootedBucket1 = bucket;
		bootedBucket2 = bucket ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

		return false;
	}

	public boolean contains( byte[] data ) {

		long hash = bucketHasher.getHash( data );

		// "dumbass cache"
		if( lastContainsHash == hash) {
			return true;
		}
		lastContainsHash = hash;

		long bucket1 = buckets.getBucket( hash );

		int fingerprint = fingerprintFromLong( hash );

		if ( buckets.contains( bucket1, fingerprint ) ) {
			return true;
		}

		long fingerprintHash = fpHasher.getHash( fingerprint );

		long bucket2 = bucket1 ^ buckets.getBucket( fingerprintHash );

		if ( buckets.contains( bucket2, fingerprint ) ) {
			return true;
		}

		return ((bucket1 == bootedBucket1 && bucket2 == bootedBucket2) || (bucket2 == bootedBucket1 && bucket1 == bootedBucket2)) && fingerprint == bootedFingerprint;
	}

	public int getBootedFingerprint() {
		return bootedFingerprint;
	}

	public long getBootedBucket1() {
		return bootedBucket1;
	}

	public long getBootedBucket2() {
		return bootedBucket2;
	}

	public long getMemoryUsageBytes() {

		return buckets.getMemoryUsageBytes();
	}

	public long getCapacity() {

		return buckets.getCapacity();
	}

	public void close() {

		buckets.delete();
	}
}