package org.cinnom.nanocuckoo;

import org.cinnom.nanocuckoo.encode.StringEncoder;
import org.cinnom.nanocuckoo.encode.UTF8Encoder;
import org.cinnom.nanocuckoo.hash.BucketHasher;
import org.cinnom.nanocuckoo.hash.FingerprintHasher;
import org.cinnom.nanocuckoo.hash.FixedHasher;
import org.cinnom.nanocuckoo.hash.XXHasher;

import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implements a Cuckoo Filter, as per "Cuckoo Filter: Practically Better Than Bloom" by Bin Fan, David G. Andersen,
 * Michael Kaminsky, Michael D. Mitzenmacher. <p> This filter uses sun.misc.Unsafe to allocate native memory. Filter
 * creation will fail if Unsafe can't be obtained, or if memory can't be allocated. </p><p> Close this filter via its
 * close() method, or a memory leak WILL occur. </p>
 */
public class NanoCuckooFilter {

	private static final int BITS_PER_BYTE = 8;
	private static final int BITS_PER_SHORT = 16;
	private static final int BITS_PER_INT = 32;
	private static final int BITS_PER_LONG = 64;

	private final UnsafeBuckets buckets;
	private final SplittableRandom random;
	private final StringEncoder stringEncoder;
	private final BucketHasher bucketHasher;
	private final FingerprintHasher fpHasher;

	private final boolean expansionEnabled;
	private final int fpBits;
	private final int fpPerLong;
	private final int fpMask;
	private final int maxKicks;
	private final int concurrencyBucketMask;
	private final ReentrantLock swapLock = new ReentrantLock();
	private final ReentrantLock[] bucketLocks;
	private final AtomicLong count = new AtomicLong();
	private final long maxFastCount;
	private final AtomicBoolean insertAllowed = new AtomicBoolean( true );

	private volatile int bootedFingerprint = -1;
	private volatile long bootedBucket = -1;

	private NanoCuckooFilter( int entriesPerBucket, long capacity, boolean expansionEnabled, int maxEntriesPerBucket,
							  int fpBits, int maxKicks, int seed, BucketHasher bucketHasher, FingerprintHasher fpHasher,
							  int concurrency, boolean countingEnabled, double smartInsertLoadFactor,
							  StringEncoder stringEncoder )
			throws NoSuchFieldException, IllegalAccessException {

		boolean countingDisabled = !countingEnabled;

		switch ( fpBits ) {
			case BITS_PER_BYTE:
				buckets = new ByteUnsafeBuckets( entriesPerBucket, capacity, maxEntriesPerBucket, countingDisabled );
				break;
			case BITS_PER_SHORT:
				buckets = new ShortUnsafeBuckets( entriesPerBucket, capacity, maxEntriesPerBucket, countingDisabled );
				break;
			case BITS_PER_INT:
				buckets = new IntUnsafeBuckets( entriesPerBucket, capacity, maxEntriesPerBucket, countingDisabled );
				break;
			default:
				buckets = new VariableUnsafeBuckets( entriesPerBucket, capacity, maxEntriesPerBucket, fpBits, countingDisabled );
				break;
		}

		random = new SplittableRandom( seed );

		this.stringEncoder = stringEncoder;
		this.bucketHasher = bucketHasher;
		this.fpHasher = fpHasher;

		this.expansionEnabled = expansionEnabled;
		this.fpBits = fpBits;

		fpPerLong = BITS_PER_LONG / fpBits;

		int shift = BITS_PER_INT - fpBits;
		fpMask = -1 >>> shift;

		this.maxKicks = maxKicks;

		if ( buckets.getBucketCount() < Integer.MAX_VALUE ) {
			concurrency = Math.min( concurrency, (int) buckets.getBucketCount() );
		}
		this.concurrencyBucketMask = concurrency - 1;

		bucketLocks = new ReentrantLock[concurrency];
		for ( int i = 0; i < concurrency; i++ ) {
			bucketLocks[i] = new ReentrantLock();
		}

		this.maxFastCount = (long) ( buckets.getCapacity() * smartInsertLoadFactor );
	}

	/**
	 * Insert a String into the filter. Will use the initially set String encoder (UTF8Encoder by default).
	 *
	 * @param value        String to insert.
	 * @param insertSafety Insert safety/speed.
	 * @return True if value successfully inserted, false if filter is full.
	 */
	public boolean insert( String value, InsertSafety insertSafety ) {

		return insert( stringEncoder.encode( value ), insertSafety );
	}

	/**
	 * Insert a byte array into the filter.
	 *
	 * @param data         Data to insert.
	 * @param insertSafety Insert safety/speed.
	 * @return True if value successfully inserted, false if filter is full.
	 */
	public boolean insert( byte[] data, InsertSafety insertSafety ) {

		return insert( bucketHasher.getHash( data ), insertSafety );
	}

	/**
	 * Insert a pre-hashed value into the filter.
	 *
	 * @param hash         Hash to insert.
	 * @param insertSafety Insert safety/speed.
	 * @return True if value successfully inserted, false if filter is full.
	 */
	public boolean insert( long hash, InsertSafety insertSafety ) {

		if ( insertAllowed.get() ) {

			long bucket1 = buckets.getBucket( hash );

			int fingerprint = fingerprintFromLong( hash );

			boolean fast = true;

			if ( insertSafety == InsertSafety.NORMAL || ( insertSafety == InsertSafety.SMART && count.get() > maxFastCount ) ) {
				fast = false;
			}

			if ( insertFingerprint( fingerprint, bucket1, fast ) ) {
				count.incrementAndGet();
				return true;
			}
		}

		return false;
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

	private void lockBucket( long bucket ) {

		bucketLocks[(int) bucket & concurrencyBucketMask].lock();
	}

	private void unlockBucket( long bucket ) {

		bucketLocks[(int) bucket & concurrencyBucketMask].unlock();
	}

	private boolean insertFingerprint( int fingerprint, long bucket1, boolean fast ) {

		try {
			lockBucket( bucket1 );
			if ( buckets.insert( bucket1, fingerprint ) ) {
				return true;
			}
		} finally {
			unlockBucket( bucket1 );
		}

		long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

		try {
			lockBucket( bucket2 );
			if ( buckets.insert( bucket2, fingerprint ) ) {
				return true;
			}
		} finally {
			unlockBucket( bucket2 );
		}

		if ( fast ) {
			return fastSwap( fingerprint, bucket2 );
		}

		try {
			swapLock.lock();

			if ( insertAllowed.get() ) {

				bootedFingerprint = fingerprint;
				bootedBucket = bucket2;

				return safeSwap();
			}

		} finally {
			swapLock.unlock();
		}

		return false;
	}

	private boolean safeSwap() {

		for ( int n = 0; n < maxKicks; n++ ) {

			int entrySwap = random.nextInt() & buckets.getEntryMask();

			try {
				lockBucket( bootedBucket );
				bootedFingerprint = buckets.swap( entrySwap, bootedBucket, bootedFingerprint );
			} finally {
				unlockBucket( bootedBucket );
			}

			bootedBucket = bootedBucket ^ buckets.getBucket( fpHasher.getHash( bootedFingerprint ) );

			try {
				lockBucket( bootedBucket );
				if ( buckets.insert( bootedBucket, bootedFingerprint ) ) {
					return true;
				}
			} finally {
				unlockBucket( bootedBucket );
			}
		}

		if ( expansionEnabled && buckets.expand() ) {
			return insertFingerprint( bootedFingerprint, bootedBucket, false );
		}

		insertAllowed.set( false );

		return true;
	}

	private boolean fastSwap( int fingerprint, long bucket ) {

		for ( int n = 0; n < maxKicks; n++ ) {

			int entrySwap = random.nextInt() & buckets.getEntryMask();

			try {
				lockBucket( bucket );
				fingerprint = buckets.swap( entrySwap, bucket, fingerprint );
			} finally {
				unlockBucket( bucket );
			}

			bucket = bucket ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

			try {
				lockBucket( bucket );
				if ( buckets.insert( bucket, fingerprint ) ) {
					return true;
				}
			} finally {
				unlockBucket( bucket );
			}
		}

		if ( expansionEnabled && buckets.expand() ) {
			return insertFingerprint( fingerprint, bucket, true );
		}

		if ( insertAllowed.compareAndSet( false, true ) ) {
			bootedFingerprint = fingerprint;
			bootedBucket = bucket;
			return true;
		}

		return false;
	}

	/**
	 * Check if a given String has been inserted into the filter. Will use the initially set String encoder (UTF8Encoder
	 * by default).
	 *
	 * @param value String to check.
	 * @return True if value is in filter, false if not.
	 */
	public boolean contains( String value ) {

		return contains( stringEncoder.encode( value ) );
	}

	/**
	 * Check if a given byte array has been inserted into the filter.
	 *
	 * @param data Data to check.
	 * @return True if value is in filter, false if not.
	 */
	public boolean contains( byte[] data ) {

		long hash = bucketHasher.getHash( data );

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

	/**
	 * Count occurrences of data in filter.
	 *
	 * @param data Data to count.
	 * @return Number of times data was previously inserted.
	 */
	public int count( byte[] data ) {

		long hash = bucketHasher.getHash( data );

		int fingerprint = fingerprintFromLong( hash );
		long bucket1 = buckets.getBucket( hash );
		long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

		int count = buckets.count( bucket1, fingerprint ) + buckets.count( bucket2, fingerprint );

		if ( ( bucket1 == bootedBucket || bucket2 == bootedBucket ) && fingerprint == bootedFingerprint ) {
			count++;
		}

		return count;
	}

	/**
	 * Delete all occurrences of data in filter.
	 *
	 * @param data Data to delete.
	 * @return Number of times data was deleted.
	 */
	public int deleteAll( byte[] data ) {

		long hash = bucketHasher.getHash( data );

		int fingerprint = fingerprintFromLong( hash );
		long bucket1 = buckets.getBucket( hash );
		long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

		int deletedCount;
		try {
			lockBucket( bucket1 );
			deletedCount = buckets.deleteAll( bucket1, fingerprint );
		} finally {
			unlockBucket( bucket1 );
		}
		try {
			lockBucket( bucket2 );
			deletedCount += buckets.deleteAll( bucket2, fingerprint );
		} finally {
			unlockBucket( bucket2 );
		}

		if ( ( bucket1 == bootedBucket || bucket2 == bootedBucket ) && fingerprint == bootedFingerprint ) {
			bootedFingerprint = -1;
			bootedBucket = -1;
			deletedCount++;
		}

		return deletedCount;
	}

	/**
	 * Delete a specific number of occurrences of data in filter.
	 *
	 * @param data  Data to delete.
	 * @param count Number of occurrences to delete.
	 * @return Number of times data was deleted.
	 */
	public int deleteCount( byte[] data, int count ) {

		long hash = bucketHasher.getHash( data );

		int fingerprint = fingerprintFromLong( hash );
		long bucket1 = buckets.getBucket( hash );

		int deletedCount;
		try {
			lockBucket( bucket1 );
			deletedCount = buckets.deleteCount( bucket1, fingerprint, count );
		} finally {
			unlockBucket( bucket1 );
		}

		int remaining = count - deletedCount;
		if ( remaining > 0 ) {

			long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

			try {
				lockBucket( bucket2 );
				deletedCount += buckets.deleteCount( bucket2, fingerprint, remaining );
			} finally {
				unlockBucket( bucket2 );
			}

			if ( deletedCount < count ) {
				if ( ( bucket1 == bootedBucket || bucket2 == bootedBucket ) && fingerprint == bootedFingerprint ) {
					bootedFingerprint = -1;
					bootedBucket = -1;
					deletedCount++;
				}
			}
		}
		return deletedCount;
	}

	/**
	 * Delete one occurrence of data in filter.
	 *
	 * @param data Data to delete.
	 * @return True if data was deleted, false if not.
	 */
	public boolean delete( byte[] data ) {

		long hash = bucketHasher.getHash( data );

		long bucket1 = buckets.getBucket( hash );

		int fingerprint = fingerprintFromLong( hash );

		try {
			lockBucket( bucket1 );
			if ( buckets.delete( bucket1, fingerprint ) ) {
				return true;
			}
		} finally {
			unlockBucket( bucket1 );
		}

		long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

		try {
			lockBucket( bucket2 );
			if ( buckets.delete( bucket2, fingerprint ) ) {
				return true;
			}
		} finally {
			unlockBucket( bucket2 );
		}

		if ( ( bucket1 == bootedBucket || bucket2 == bootedBucket ) && fingerprint == bootedFingerprint ) {
			bootedFingerprint = -1;
			bootedBucket = -1;
			return true;
		}

		return false;
	}

	/**
	 * @return Native memory bytes used by filter.
	 */
	public long getMemoryUsageBytes() {

		return buckets.getMemoryUsageBytes();
	}

	/**
	 * @return Filter maximum capacity.
	 */
	public long getCapacity() {

		return buckets.getCapacity();
	}

	/**
	 * @return Filter load factor.
	 */
	public double getLoadFactor() {

		return (double) count.get() / buckets.getCapacity();
	}

	/**
	 * Close this filter. This needs to be called to deallocate native memory.
	 */
	public void close() {

		buckets.close();
	}

	/**
	 * Builder for NanoCuckooFilter.
	 */
	public static class Builder {

		private static final int POS_INT = 0x7FFFFFFF;
		private final long capacity;
		private int entriesPerBucket = 4;
		private boolean expansionEnabled = false;
		private int maxEntriesPerBucket = 8;
		private int fpBits = 8;
		private int maxKicks = 400;
		private int seed = 0x48F7E28A;
		private int concurrency = 64;
		private boolean countingEnabled = false;
		private double smartInsertLoadFactor = 0.90;
		private StringEncoder stringEncoder = new UTF8Encoder();
		private BucketHasher bucketHasher = new XXHasher( seed );
		private FingerprintHasher fpHasher = new FixedHasher();

		/**
		 * Instantiate a NanoCuckooFilter.Builder with the given bucket count. If the given bucket count is not a power
		 * of 2, it will be scaled up. Resulting max capacity will be bucket count multiplied by entries per bucket.
		 *
		 * @param bucketCount Desired filter bucket count.
		 */
		public Builder( long bucketCount ) {

			if ( bucketCount <= 0 ) {
				throw new IllegalArgumentException( "Bucket count must be positive" );
			}

			this.capacity = bucketCount;
		}

		/**
		 * Build the filter using provided parameters. Will return null if sun.misc.Unsafe couldn't be obtained.
		 *
		 * @return NanoCuckooFilter, or null.
		 */
		public NanoCuckooFilter build() {

			try {
				return new NanoCuckooFilter( entriesPerBucket, capacity, expansionEnabled, maxEntriesPerBucket, fpBits, maxKicks, seed,
						bucketHasher, fpHasher, concurrency, countingEnabled, smartInsertLoadFactor, stringEncoder );
			} catch ( NoSuchFieldException | IllegalAccessException e ) {
				// Failed trying to obtain Unsafe. Shouldn't happen, return null if it does.
			}

			return null;
		}

		/**
		 * Set entries per bucket.
		 * Must be a power of 2. Defaults to 4.
		 *
		 * @param entriesPerBucket Entries per bucket.
		 * @return Updated Builder
		 */
		public Builder withEntriesPerBucket( int entriesPerBucket ) {

			if ( Integer.bitCount( entriesPerBucket & POS_INT ) != 1 ) {
				throw new IllegalArgumentException( "Entries Per Bucket must be a power of 2" );
			}

			this.entriesPerBucket = entriesPerBucket;
			return this;
		}

		/**
		 * Set SMART insert load factor.
		 * This is the maximum load factor to use for FAST inserts before switching to NORMAL.
		 * Must be between 0 and 1. Defaults to 0.90 (90%).
		 *
		 * @param smartInsertLoadFactor SMART insert load factor.
		 * @return Updated Builder
		 */
		public Builder withSmartInsertLoadFactor( double smartInsertLoadFactor ) {

			if ( smartInsertLoadFactor < 0 || smartInsertLoadFactor > 1 ) {
				throw new IllegalArgumentException( "Smart Insert Load Factor must be between 0 and 1" );
			}

			this.smartInsertLoadFactor = smartInsertLoadFactor;
			return this;
		}

		/**
		 * Set expansion enabled. If expansion is enabled, entries per bucket will double up to MaxEntriesPerBucket when
		 * max load is hit. This will also roughly double the FPP. Defaults to false.
		 *
		 * @param expansionEnabled Expansion enabled.
		 * @return Updated Builder
		 */
		public Builder withExpansionEnabled( boolean expansionEnabled ) {

			this.expansionEnabled = expansionEnabled;
			return this;
		}

		/**
		 * Set max entries per bucket.
		 * If expansion is enabled, entries per bucket will double up to MaxEntriesPerBucket when max load is hit.
		 * This will also roughly double the FPP.
		 * Must be a power of 2. Defaults to 8.
		 *
		 * @param maxEntriesPerBucket Max entries per bucket.
		 * @return Updated Builder
		 */
		public Builder withMaxEntriesPerBucket( int maxEntriesPerBucket ) {

			if ( Integer.bitCount( maxEntriesPerBucket & POS_INT ) != 1 ) {
				throw new IllegalArgumentException( "Maximum Entries Per Bucket must be a power of 2" );
			}

			this.maxEntriesPerBucket = maxEntriesPerBucket;
			return this;
		}

		/**
		 * Set fingerprint bits.
		 * More bits means lower FPP, but also more memory used.
		 * Must be from 1 to 32. Defaults to 8.
		 *
		 * @param fpBits Fingerprint bits.
		 * @return Updated Builder
		 */
		public Builder withFingerprintBits( int fpBits ) {

			if ( fpBits < 1 || fpBits > 32 ) {
				throw new IllegalArgumentException( "Fingerprint Bits must be from 1 to 32" );
			}

			this.fpBits = fpBits;
			return this;
		}

		/**
		 * Set max kicks. Higher will result in higher load factor before insert failure, but worse insert performance
		 * as max load is approached. Defaults to 400 (results in around 95% LF).
		 *
		 * @param maxKicks Max kicks.
		 * @return Updated Builder
		 */
		public Builder withMaxKicks( int maxKicks ) {

			if ( maxKicks < 0 ) {
				throw new IllegalArgumentException( "Maximum Kicks must be at least zero" );
			}

			this.maxKicks = maxKicks;
			return this;
		}

		/**
		 * Set random seed.
		 * Used when randomly swapping entries.
		 * Defaults to 0x48F7E28A.
		 *
		 * @param seed Random seed.
		 * @return Updated Builder
		 */
		public Builder withRandomSeed( int seed ) {

			this.seed = seed;
			return this;
		}

		/**
		 * Set String encoder.
		 * Used by String insert/contains/count/delete.
		 * Defaults to UTF8Encoder.
		 *
		 * @param stringEncoder String encoder.
		 * @return Updated Builder
		 */
		public Builder withStringEncoder( StringEncoder stringEncoder ) {

			if ( stringEncoder == null ) {
				throw new IllegalArgumentException( "String Encoder must not be null" );
			}

			this.stringEncoder = stringEncoder;
			return this;
		}

		/**
		 * Set bucket hasher.
		 * Defaults to XXHasher.
		 *
		 * @param bucketHasher Bucket hasher.
		 * @return Updated Builder
		 */
		public Builder withBucketHasher( BucketHasher bucketHasher ) {

			if ( bucketHasher == null ) {
				throw new IllegalArgumentException( "Bucket BucketHasher must not be null" );
			}

			this.bucketHasher = bucketHasher;
			return this;
		}

		/**
		 * Set fingerprint hasher.
		 * Defaults to FixedHasher.
		 *
		 * @param fpHasher Fingerprint hasher.
		 * @return Updated Builder
		 */
		public Builder withFingerprintHasher( FingerprintHasher fpHasher ) {

			if ( fpHasher == null ) {
				throw new IllegalArgumentException( "Fingerprint BucketHasher must not be null" );
			}

			this.fpHasher = fpHasher;
			return this;
		}

		/**
		 * Set concurrency. Recommended to set to at least the number of Threads that will be inserting/deleting at
		 * once. A number ReentrantLocks will be allocated equal to this number. Capped by the number of buckets.
		 * Defaults to 64.
		 *
		 * @param concurrency Concurrency.
		 * @return Updated Builder
		 */
		public Builder withConcurrency( int concurrency ) {

			if ( Integer.bitCount( concurrency & POS_INT ) != 1 ) {
				throw new IllegalArgumentException( "Concurrency must be a power of 2" );
			}

			this.concurrency = concurrency;
			return this;
		}

		/**
		 * Set counting enabled. If counting is enabled, a single value can be inserted multiple times. Note that a
		 * value should not be inserted more times than (entries per bucket * 2 - 1), or filter failure can occur. If
		 * counting is disabled, it is still possible for a value to end up in the filter multiple times, but an effort
		 * will be made to avoid it. Insert operations will return true if a duplicate is detected, as if the insert
		 * succeeded.
		 *
		 * @param countingEnabled Counting enabled.
		 * @return Updated Builder.
		 */
		public Builder withCountingEnabled( boolean countingEnabled ) {

			this.countingEnabled = countingEnabled;
			return this;
		}
	}
}
