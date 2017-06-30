package org.cinnom.nanocuckoo;

import java.nio.charset.StandardCharsets;
import java.util.SplittableRandom;
import java.util.concurrent.locks.ReentrantLock;

import org.cinnom.nanocuckoo.hash.BucketHasher;
import org.cinnom.nanocuckoo.hash.FingerprintHasher;
import org.cinnom.nanocuckoo.hash.FixedHasher;
import org.cinnom.nanocuckoo.hash.XXHasher;

/**
 * Created by rjones on 6/22/17.
 */
public class NanoCuckooFilter {

	private static final int BITS_PER_INT = 32;
	private static final int BITS_PER_LONG = 64;

	private final UnsafeBuckets buckets;

	private final SplittableRandom random;

	private final BucketHasher bucketHasher;
	private final FingerprintHasher fpHasher;

	private final boolean allowExpansion;
	private final int fpBits;
	private final int fpPerLong;
	private final int fpMask;
	private final int maxKicks;

	private boolean insertAllowed = true;

	private long lastContainsHash;
	private int bootedFingerprint;
	private long bootedBucket;

	private static final int CONCURRENCY = 128;
	private static final int CONCURRENCY_BUCKET_MASK = CONCURRENCY - 1;
	private ReentrantLock[] bucketLocks = new ReentrantLock[CONCURRENCY];

	private NanoCuckooFilter( int entriesPerBucket, long capacity, boolean allowExpansion, int maxEntriesPerBucket, int fpBits,
			int maxKicks, int seed, BucketHasher bucketHasher, FingerprintHasher fpHasher )
			throws NoSuchFieldException, IllegalAccessException {

		switch ( fpBits ) {
			case 8:
				buckets = new ByteUnsafeBuckets( entriesPerBucket, capacity, maxEntriesPerBucket );
				break;
			case 16:
				buckets = new ShortUnsafeBuckets( entriesPerBucket, capacity, maxEntriesPerBucket );
				break;
			case 32:
				buckets = new IntUnsafeBuckets( entriesPerBucket, capacity, maxEntriesPerBucket );
				break;
			default:
				buckets = new VariableUnsafeBuckets( entriesPerBucket, capacity, maxEntriesPerBucket, fpBits );
				break;
		}

		random = new SplittableRandom( seed );

		this.bucketHasher = bucketHasher;
		this.fpHasher = fpHasher;

		this.allowExpansion = allowExpansion;
		this.fpBits = fpBits;

		fpPerLong = BITS_PER_LONG / fpBits;

		int shift = BITS_PER_INT - fpBits;
		fpMask = -1 >>> shift;

		this.maxKicks = maxKicks;

		for(int i = 0; i < CONCURRENCY; i++) {
			bucketLocks[i] = new ReentrantLock(  );
		}
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

	private void lockBucket(long bucket) {
		bucketLocks[(int) bucket & CONCURRENCY_BUCKET_MASK].lock();
	}

	private void unlockBucket(long bucket) {
		bucketLocks[(int) bucket & CONCURRENCY_BUCKET_MASK].unlock();
	}

	private boolean insertFingerprint( int fingerprint, long bucket1 ) {

		lockBucket(bucket1);
		if ( buckets.insert( bucket1, fingerprint, true ) ) {
			unlockBucket(bucket1);
			return true;
		}
		unlockBucket(bucket1);

		long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

		lockBucket(bucket2);
		if ( buckets.insert( bucket2, fingerprint, true ) ) {
			unlockBucket(bucket2);
			return true;
		}
		unlockBucket(bucket2);

		long bucket = bucket2;

		for ( int n = 0; n < maxKicks; n++ ) {

			int entrySwap = random.nextInt() & buckets.getEntryMask();
			lockBucket(bucket);
			fingerprint = buckets.swap( entrySwap, bucket, fingerprint );
			unlockBucket(bucket);

			bucket = bucket ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

			lockBucket(bucket);
			if ( buckets.insert( bucket, fingerprint, true ) ) {
				unlockBucket(bucket);
				return true;
			}
			unlockBucket(bucket);
		}

		if ( allowExpansion && buckets.expand() ) {
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

		long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

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

	public static class Builder {

		private final long capacity;
		private int entriesPerBucket = 4;
		private boolean allowExpansion = false;
		private int maxEntriesPerBucket = 8;
		private int fpBits = 8;
		private int maxKicks = 256;
		private int seed = 0x48F7E28A;
		private BucketHasher bucketHasher = new XXHasher( seed );
		private FingerprintHasher fpHasher = new FixedHasher();

		public Builder( long capacity ) {

			if(capacity <= 0) {
				throw new IllegalArgumentException( "Capacity must be positive" );
			}

			this.capacity = capacity;
		}

		public Builder withEntriesPerBucket( int entriesPerBucket ) {

			if(entriesPerBucket <= 0) {
				throw new IllegalArgumentException( "Entries Per Bucket must be positive" );
			}

			if(Integer.bitCount( entriesPerBucket ) != 1) {
				throw new IllegalArgumentException( "Entries Per Bucket be a power of 2" );
			}

			this.entriesPerBucket = entriesPerBucket;
			return this;
		}

		public Builder withAllowBucketExpansion( boolean allowExpansion ) {

			this.allowExpansion = allowExpansion;
			return this;
		}

		public Builder withMaxEntriesPerBucket( int maxEntriesPerBucket ) {

			if(maxEntriesPerBucket <= 0) {
				throw new IllegalArgumentException( "Maximum Entries Per Bucket must be positive" );
			}

			if(Integer.bitCount( maxEntriesPerBucket ) != 1) {
				throw new IllegalArgumentException( "Maximum Entries Per Bucket be a power of 2" );
			}

			this.maxEntriesPerBucket = maxEntriesPerBucket;
			return this;
		}

		public Builder withFingerprintBits( int fpBits ) {

			if(fpBits < 0 || fpBits > 32) {
				throw new IllegalArgumentException( "Fingerprint Bits must be from 1 to 32" );
			}

			this.fpBits = fpBits;
			return this;
		}

		public Builder withMaxKicks( int maxKicks ) {

			if(maxKicks < 0) {
				throw new IllegalArgumentException( "Maximum Kicks must be at least zero" );
			}

			this.maxKicks = maxKicks;
			return this;
		}

		public Builder withRandomSeed( int seed ) {

			this.seed = seed;
			return this;
		}

		public Builder withBucketHasher( BucketHasher bucketHasher ) {

			if(bucketHasher == null) {
				throw new IllegalArgumentException( "Bucket Hasher must not be null" );
			}

			this.bucketHasher = bucketHasher;
			return this;
		}

		public Builder withFingerprintHasher( FingerprintHasher fpHasher ) {

			if(fpHasher == null) {
				throw new IllegalArgumentException( "Bucket Hasher must not be null" );
			}

			this.fpHasher = fpHasher;
			return this;
		}

		public NanoCuckooFilter build() {

			try {
				return new NanoCuckooFilter( entriesPerBucket, capacity, allowExpansion, maxEntriesPerBucket, fpBits, maxKicks, seed,
						bucketHasher, fpHasher );
			} catch ( NoSuchFieldException | IllegalAccessException e ) {
				// Failed trying to obtain Unsafe. Shouldn't happen, return null if it does.
			}

			return null;
		}
	}
}
