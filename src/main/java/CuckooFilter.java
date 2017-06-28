import java.nio.charset.StandardCharsets;
import java.util.SplittableRandom;

/**
 * Created by rjones on 6/22/17.
 */
public class CuckooFilter {

	private static final int MAX_NUM_KICKS = 128;

	private static final int BYTES_PER_LONG = 8;

	private static int seed = 0x48f7e28a;

	private final ByteUnsafeBuckets buckets;

	private final SplittableRandom random;

	private boolean allowExpand;

	private byte bootedFingerprint;
	private long bootedBucket1;
	private long bootedBucket2;

	boolean insertAllowed = true;

	public CuckooFilter( int entries, long capacity, boolean allowExpand, int maxEntries ) throws NoSuchFieldException, IllegalAccessException {

		buckets = new ByteUnsafeBuckets( entries, capacity, maxEntries );
		random = new SplittableRandom( seed );
		this.allowExpand = allowExpand;
	}

	public boolean insert( String value ) {

		final byte[] data = value.getBytes( StandardCharsets.UTF_8 ); // optimize

		return insert( data );
	}

	public boolean insert( byte[] data ) {

		if(!insertAllowed) {
			return false;
		}

		long hash = XXHasher.getHash( data );

		long bucket1 = buckets.getBucket( hash );

		byte fingerprint = fingerprintFromLong( hash );

		insertAllowed = insertFingerprint( fingerprint, bucket1 );

		return insertAllowed;
	}

	private byte fingerprintFromLong(long hash) {

		for(int i = 0; i < BYTES_PER_LONG; i++) {
			byte tempFp = (byte) hash;
			if(tempFp != 0) {
				return tempFp;
			}
			hash >>>= 8;
		}

		return 1;
	}

	private boolean insertFingerprint(byte fingerprint, long bucket1) {

		QuickLogger.log( "Inserting Fingerprint: " + fingerprint );

		if ( buckets.insert( bucket1, fingerprint, true ) ) {
			return true;
		}

		long fingerprintHash = XXHasher.getHash( fingerprint );

		long bucket2 = bucket1 ^ buckets.getBucket( fingerprintHash );

		if ( buckets.insert( bucket2, fingerprint, true ) ) {
			return true;
		}

		// long bucket = random.nextBoolean() ? bucket1 : bucket2;
		long bucket = bucket2;

		for ( int n = 0; n < MAX_NUM_KICKS; n++ ) {

			int entrySwap = Math.abs( random.nextInt( buckets.getEntries() ) );
			fingerprint = buckets.swap( entrySwap, bucket, fingerprint );
			QuickLogger.log( "Swapped out fingerprint: " + fingerprint );
			bucket = bucket ^ buckets.getBucket( XXHasher.getHash( fingerprint ) );
			if ( buckets.insert( bucket, fingerprint, true ) ) {
				return true;
			}
		}

		if(allowExpand && buckets.expand()) {
			return insertFingerprint( fingerprint, bucket );
		}

		bootedFingerprint = fingerprint;
		bootedBucket1 = bucket;
		bootedBucket2 = bucket ^ buckets.getBucket( XXHasher.getHash( fingerprint ) );

		return false;
	}

	public boolean contains( byte[] data ) {

		long hash = XXHasher.getHash( data );

		long bucket1 = buckets.getBucket( hash );

		byte fingerprint = fingerprintFromLong( hash );

		if ( buckets.contains( bucket1, fingerprint ) ) {
			return true;
		}

		long fingerprintHash = XXHasher.getHash( fingerprint );

		long bucket2 = bucket1 ^ buckets.getBucket( fingerprintHash );

		if ( buckets.contains( bucket2, fingerprint ) ) {
			return true;
		}

		return ((bucket1 == bootedBucket1 && bucket2 == bootedBucket2) || (bucket2 == bootedBucket1 && bucket1 == bootedBucket2)) && fingerprint == bootedFingerprint;
	}

	public byte getBootedFingerprint() {
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

	public void close() {

		buckets.delete();
	}
}
