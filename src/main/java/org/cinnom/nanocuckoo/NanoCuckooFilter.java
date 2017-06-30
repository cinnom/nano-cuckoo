package org.cinnom.nanocuckoo;

import java.nio.charset.StandardCharsets;
import java.util.SplittableRandom;

import org.cinnom.nanocuckoo.buckets.Buckets;
import org.cinnom.nanocuckoo.buckets.internal.ByteUnsafeBuckets;
import org.cinnom.nanocuckoo.buckets.internal.ConfigurableUnsafeBuckets;
import org.cinnom.nanocuckoo.buckets.internal.IntUnsafeBuckets;
import org.cinnom.nanocuckoo.buckets.internal.ShortUnsafeBuckets;
import org.cinnom.nanocuckoo.hash.*;

/**
 * Created by rjones on 6/22/17.
 */
public class NanoCuckooFilter {

	private static final int MAX_NUM_KICKS = 256;

	private static final int BITS_PER_INT = 32;
	private static final int BITS_PER_LONG = 64;

	private static int seed = 0x48F7E28A;

	private final Buckets buckets;
	private final BucketHasher bucketHasher;
	private final FingerprintHasher fpHasher;

	private final SplittableRandom random;

	private boolean allowExpand;

	private long lastContainsHash;
	private int bootedFingerprint;
	private long bootedBucket;

	boolean insertAllowed = true;

	private final int fpBits;
	private final int fpPerLong;
	private final int fpMask;

	public NanoCuckooFilter( int entryBits, long capacity, boolean allowExpand, int maxEntries, int fpBits) throws NoSuchFieldException, IllegalAccessException {

		switch ( fpBits ) {
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

		bucketHasher = xxHasher;
		random = new SplittableRandom(seed);

		this.allowExpand = allowExpand;
		this.fpBits = fpBits;

		fpPerLong = BITS_PER_LONG / fpBits;

		int shift = BITS_PER_INT - fpBits;
		fpMask = -1 >>> shift;

		fpHasher = new FixedHasher();
	}

	public boolean insert( String value ) {

		final byte[] data = value.getBytes( StandardCharsets.UTF_8 ); // optimize

		return insert( data );
	}

	public boolean contains( String value ) {

		final byte[] data = value.getBytes( StandardCharsets.UTF_8 ); // optimize

		return contains( data );
	}

	public boolean insert( byte[] data ) {

		if ( !insertAllowed ) {
			return false;
		}

		long hash = bucketHasher.getHash( data );

		long bucket1 = buckets.getBucket( hash );

		int fingerprint = fingerprintFromLong( hash );

		insertAllowed = insertFingerprint( fingerprint, bucket1 );

		return insertAllowed;
	}

	private int fingerprintFromLong( long hash ) {

		for ( int i = 0; i < fpPerLong; i++ ) {

			int tempFp = ( (int) hash ) & fpMask;
			if ( tempFp != 0 ) {
				return tempFp;
			}
			hash >>>= fpBits;
		}

		return 1;
	}

	public int getDuplicates() {
		return buckets.getDuplicates();
	}

	private boolean insertFingerprint( int fingerprint, long bucket1 ) {

		if ( buckets.insert( bucket1, fingerprint, true ) ) {
			return true;
		}

		long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

		if ( buckets.insert( bucket2, fingerprint, true ) ) {
			return true;
		}

		long bucket = bucket2;

		for ( int n = 0; n < MAX_NUM_KICKS; n++ ) {

			int entrySwap = random.nextInt() & buckets.getEntryMask();
			fingerprint = buckets.swap( entrySwap, bucket, fingerprint );

			bucket = bucket ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

			if ( buckets.insert( bucket, fingerprint, true ) ) {
				return true;
			}
		}

		if ( allowExpand && buckets.expand() ) {
			return insertFingerprint( fingerprint, bucket );
		}

		bootedFingerprint = fingerprint;
		bootedBucket = bucket;

		return false;
	}

	public boolean contains( byte[] data ) {

		long hash = bucketHasher.getHash( data );

		// "dumbass cache"
		if ( lastContainsHash == hash ) {
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

		return ( bucket1 == bootedBucket || bucket2 == bootedBucket ) && fingerprint == bootedFingerprint;
	}

	public int getBootedFingerprint() {

		return bootedFingerprint;
	}

	public long getBootedBucket() {

		return bootedBucket;
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
