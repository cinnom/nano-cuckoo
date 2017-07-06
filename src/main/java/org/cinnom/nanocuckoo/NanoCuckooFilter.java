package org.cinnom.nanocuckoo;

import org.cinnom.nanocuckoo.encode.StringEncoder;
import org.cinnom.nanocuckoo.encode.UTF8Encoder;
import org.cinnom.nanocuckoo.hash.BucketHasher;
import org.cinnom.nanocuckoo.hash.FingerprintHasher;
import org.cinnom.nanocuckoo.hash.FixedHasher;
import org.cinnom.nanocuckoo.hash.XXHasher;

/**
 * <p>
 * Implements a Cuckoo Filter, as per "Cuckoo Filter: Practically Better Than Bloom" by Bin Fan, David G. Andersen,
 * Michael Kaminsky, Michael D. Mitzenmacher.
 * </p>
 * <p>
 * This filter uses sun.misc.Unsafe to allocate native memory. Filter creation will fail if Unsafe can't be obtained, or
 * if memory can't be allocated.
 * </p>
 * <p>
 * Close this filter via its close() method, or a memory leak WILL occur.
 * </p>
 */
public class NanoCuckooFilter {

	private static final int BITS_PER_BYTE = 8;
	private static final int BITS_PER_SHORT = 16;
	private static final int BITS_PER_INT = 32;
	private static final int BITS_PER_LONG = 64;

	private final UnsafeBuckets buckets;
	private final StringEncoder stringEncoder;
	private final BucketHasher bucketHasher;
	private final FingerprintHasher fpHasher;

	private final int fpBits;
	private final int fpPerLong;
	private final int fpMask;

	private final KickedValues kickedValues = new KickedValues();
	private final BucketLocker bucketLocker;
	private final Swapper swapper;

	private NanoCuckooFilter( int entriesPerBucket, long capacity, int fpBits, int maxKicks, int seed,
			BucketHasher bucketHasher, FingerprintHasher fpHasher, int concurrency, boolean countingEnabled,
			double smartInsertLoadFactor, StringEncoder stringEncoder, ConcurrentSwapSafety concurrentSwapSafety )
			throws NoSuchFieldException, IllegalAccessException {

		boolean countingDisabled = !countingEnabled;

		long bucketCount = capacity / entriesPerBucket;

		switch ( fpBits ) {
			case BITS_PER_BYTE:
				buckets = new ByteUnsafeBuckets( entriesPerBucket, bucketCount, countingDisabled );
				break;
			case BITS_PER_SHORT:
				buckets = new ShortUnsafeBuckets( entriesPerBucket, bucketCount, countingDisabled );
				break;
			case BITS_PER_INT:
				buckets = new IntUnsafeBuckets( entriesPerBucket, bucketCount, countingDisabled );
				break;
			default:
				buckets = new VariableUnsafeBuckets( entriesPerBucket, bucketCount, fpBits, countingDisabled );
				break;
		}

		this.stringEncoder = stringEncoder;
		this.bucketHasher = bucketHasher;
		this.fpHasher = fpHasher;
		this.fpBits = fpBits;

		this.bucketLocker = new BucketLocker( concurrency, buckets.getBucketCount() );

		// Set how many potential fingerprints we can locate in a bucket hash
		fpPerLong = BITS_PER_LONG / fpBits;

		// Set the mask that will pull fingerprint bits from the bucket hash
		fpMask = -1 >>> ( BITS_PER_INT - fpBits );

		switch ( concurrentSwapSafety ) {

			case FAST:
				swapper = new FastSwapper( kickedValues, bucketLocker, buckets, fpHasher, maxKicks, seed );
				break;
			case SMART:
				swapper = new SmartSwapper( kickedValues, bucketLocker, buckets, fpHasher, maxKicks, seed,
						smartInsertLoadFactor );
				break;
			case RELIABLE:
			default:
				swapper = new ReliableSwapper( kickedValues, bucketLocker, buckets, fpHasher, maxKicks, seed );
				break;
		}
	}

	/**
	 * Insert a String into the filter. Will use the initially set String encoder (UTF8Encoder by default).
	 *
	 * @param value
	 *            String to insert.
	 * @return True if value successfully inserted, false if filter is full.
	 */
	public boolean insert( String value ) {

		return insert( stringEncoder.encode( value ) );
	}

	/**
	 * Insert a byte array into the filter.
	 *
	 * @param data
	 *            Data to insert.
	 * @return True if value successfully inserted, false if filter is full.
	 */
	public boolean insert( byte[] data ) {

		return insert( bucketHasher.getHash( data ) );
	}

	/**
	 * Insert a pre-hashed value into the filter.
	 *
	 * @param hash
	 *            Hash to insert.
	 * @return True if value successfully inserted, false if filter is full.
	 */
	public boolean insert( long hash ) {

		long bucket1 = buckets.getBucket( hash );
		int fingerprint = fingerprintFromLong( hash );

		return insertFingerprint( fingerprint, bucket1 );
	}

	/**
	 * Check if a given String has been inserted into the filter. Will use the initially set String encoder (UTF8Encoder
	 * by default).
	 *
	 * @param value
	 *            String to check.
	 * @return True if value is in filter, false if not.
	 */
	public boolean contains( String value ) {

		return contains( stringEncoder.encode( value ) );
	}

	/**
	 * Check if a given byte array has been inserted into the filter.
	 *
	 * @param data
	 *            Data to check.
	 * @return True if value is in filter, false if not.
	 */
	public boolean contains( byte[] data ) {

		return contains( bucketHasher.getHash( data ) );
	}

	/**
	 * Check if a given pre-hashed value has been inserted into the filter.
	 *
	 * @param hash
	 *            Hash to check.
	 * @return True if hash is in filter, false if not.
	 */
	public boolean contains( long hash ) {

		long bucket1 = buckets.getBucket( hash );

		int fingerprint = fingerprintFromLong( hash );

		if ( buckets.contains( bucket1, fingerprint ) ) {
			return true;
		}

		long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

		return buckets.contains( bucket2, fingerprint ) || kickedValues.equals( fingerprint, bucket1, bucket2 );
	}

	/**
	 * Count occurrences of a given String in filter. Will use the initially set String encoder (UTF8Encoder by
	 * default).
	 *
	 * @param value
	 *            Value to count.
	 * @return Number of times value was previously inserted.
	 */
	public int count( String value ) {

		return count( stringEncoder.encode( value ) );
	}

	/**
	 * Count occurrences of given byte data in filter.
	 *
	 * @param data
	 *            Data to count.
	 * @return Number of times data was previously inserted.
	 */
	public int count( byte[] data ) {

		return count( bucketHasher.getHash( data ) );
	}

	/**
	 * Count occurrences of a given pre-hashed value in filter.
	 *
	 * @param hash
	 *            Hash to count.
	 * @return Number of times hash was previously inserted.
	 */
	public int count( long hash ) {

		int fingerprint = fingerprintFromLong( hash );
		long bucket1 = buckets.getBucket( hash );
		long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

		int count = buckets.count( bucket1, fingerprint ) + buckets.count( bucket2, fingerprint );

		if ( kickedValues.equals( fingerprint, bucket1, bucket2 ) ) {
			count++;
		}

		return count;
	}

	/**
	 * Delete all occurrences of a given String in filter. Will use the initially set String encoder (UTF8Encoder by
	 * default).
	 *
	 * @param value
	 *            Value to delete.
	 * @return Number of times value was deleted.
	 */
	public int deleteAll( String value ) {

		return deleteAll( stringEncoder.encode( value ) );
	}

	/**
	 * Delete all occurrences of given byte data in filter.
	 *
	 * @param data
	 *            Data to delete.
	 * @return Number of times data was deleted.
	 */
	public int deleteAll( byte[] data ) {

		return deleteAll( bucketHasher.getHash( data ) );
	}

	/**
	 * Delete all occurrences of a given pre-hashed value in filter.
	 *
	 * @param hash
	 *            hash to delete.
	 * @return Number of times hash was deleted.
	 */
	public int deleteAll( long hash ) {

		int fingerprint = fingerprintFromLong( hash );
		long bucket1 = buckets.getBucket( hash );
		long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

		int deletedCount;
		try {
			bucketLocker.lockBucket( bucket1 );
			deletedCount = buckets.deleteAll( bucket1, fingerprint );
		} finally {
			bucketLocker.unlockBucket( bucket1 );
		}
		try {
			bucketLocker.lockBucket( bucket2 );
			deletedCount += buckets.deleteAll( bucket2, fingerprint );
		} finally {
			bucketLocker.unlockBucket( bucket2 );
		}

		kickedValues.lock();
		if ( kickedValues.equals( fingerprint, bucket1, bucket2 ) ) {
			kickedValues.clear();
			deletedCount++;
		}
		reinsertKickedFingerprint();
		kickedValues.unlock();

		return deletedCount;
	}

	/**
	 * Delete a specific number of occurrences of a given String in filter. Will use the initially set String encoder
	 * (UTF8Encoder by default).
	 *
	 * @param value
	 *            Value to delete.
	 * @param count
	 *            Number of occurrences to delete.
	 * @return Number of times value was deleted.
	 */
	public int deleteCount( String value, int count ) {

		return deleteCount( stringEncoder.encode( value ), count );
	}

	/**
	 * Delete a specific number of occurrences of given byte data in filter.
	 *
	 * @param data
	 *            Data to delete.
	 * @param count
	 *            Number of occurrences to delete.
	 * @return Number of times data was deleted.
	 */
	public int deleteCount( byte[] data, int count ) {

		return deleteCount( bucketHasher.getHash( data ), count );
	}

	/**
	 * Delete a specific number of occurrences of a given pre-hashed value in filter.
	 *
	 * @param hash
	 *            Hash to delete.
	 * @param count
	 *            Number of occurrences to delete.
	 * @return Number of times hash was deleted.
	 */
	public int deleteCount( long hash, int count ) {

		int fingerprint = fingerprintFromLong( hash );
		long bucket1 = buckets.getBucket( hash );

		int deletedCount;
		try {
			bucketLocker.lockBucket( bucket1 );
			deletedCount = buckets.deleteCount( bucket1, fingerprint, count );
		} finally {
			bucketLocker.unlockBucket( bucket1 );
		}

		int remaining = count - deletedCount;
		if ( remaining > 0 ) {

			long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

			try {
				bucketLocker.lockBucket( bucket2 );
				deletedCount += buckets.deleteCount( bucket2, fingerprint, remaining );
			} finally {
				bucketLocker.unlockBucket( bucket2 );
			}

			if ( deletedCount < count ) {
				kickedValues.lock();
				if ( kickedValues.equals( fingerprint, bucket1, bucket2 ) ) {
					kickedValues.clear();
					deletedCount++;
				}
				reinsertKickedFingerprint();
				kickedValues.unlock();
			}
		}
		return deletedCount;
	}

	/**
	 * Delete one occurrence of a given String in filter. Will use the initially set String encoder (UTF8Encoder by
	 * default).
	 *
	 * @param value
	 *            Valie to delete.
	 * @return True if value was deleted, false if not.
	 */
	public boolean delete( String value ) {

		return delete( stringEncoder.encode( value ) );
	}

	/**
	 * Delete one occurrence of given byte data in filter.
	 *
	 * @param data
	 *            Data to delete.
	 * @return True if data was deleted, false if not.
	 */
	public boolean delete( byte[] data ) {

		return delete( bucketHasher.getHash( data ) );
	}

	/**
	 * Delete one occurrence of a given pre-hashed value in filter.
	 *
	 * @param hash
	 *            Hash to delete.
	 * @return True if hash was deleted, false if not.
	 */
	public boolean delete( long hash ) {

		long bucket1 = buckets.getBucket( hash );

		int fingerprint = fingerprintFromLong( hash );

		try {
			bucketLocker.lockBucket( bucket1 );
			if ( buckets.delete( bucket1, fingerprint ) ) {
				return true;
			}
		} finally {
			bucketLocker.unlockBucket( bucket1 );
		}

		long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

		try {
			bucketLocker.lockBucket( bucket2 );
			if ( buckets.delete( bucket2, fingerprint ) ) {
				return true;
			}
		} finally {
			bucketLocker.unlockBucket( bucket2 );
		}

		try {
			kickedValues.lock();
			if ( kickedValues.equals( fingerprint, bucket1, bucket2 ) ) {
				kickedValues.clear();
				return true;
			}
		} finally {
			reinsertKickedFingerprint();
			kickedValues.unlock();
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

		return (double) buckets.getInsertedCount() / buckets.getCapacity();
	}

	/**
	 * Double the number of entries per bucket. This will double memory usage, and will also roughly double the max FPP.
	 */
	public void expand() {

		buckets.expand();
	}

	/**
	 * Close this filter. This needs to be called to deallocate native memory.
	 */
	public void close() {

		buckets.close();
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

	private boolean insertFingerprint( int fingerprint, long bucket1 ) {

		try {
			bucketLocker.lockBucket( bucket1 );
			if ( buckets.insert( bucket1, fingerprint ) ) {
				return true;
			}
		} finally {
			bucketLocker.unlockBucket( bucket1 );
		}

		long bucket2 = bucket1 ^ buckets.getBucket( fpHasher.getHash( fingerprint ) );

		try {
			bucketLocker.lockBucket( bucket2 );
			if ( buckets.insert( bucket2, fingerprint ) ) {
				return true;
			}
		} finally {
			bucketLocker.unlockBucket( bucket2 );
		}

		return swapper.swap( fingerprint, bucket2 );
	}

	private void reinsertKickedFingerprint() {

		if ( !kickedValues.isClear() ) {

			int kickedFingerprint = kickedValues.getKickedFingerprint();
			long kickedBucket = kickedValues.getKickedBucket();
			kickedValues.clear();
			buckets.decrementInsertedCount();

			insertFingerprint( kickedFingerprint, kickedBucket );
		}
	}

	/**
	 * Builder for NanoCuckooFilter.
	 */
	public static class Builder {

		private static final int POS_INT = 0x7FFFFFFF;
		private final long capacity;
		private int entriesPerBucket = 4;
		private int fpBits = 8;
		private int maxKicks = 400;
		private int seed = 0x48F7E28A;
		private int concurrency = 64;
		private boolean countingEnabled = false;
		private double smartInsertLoadFactor = 0.90;
		private ConcurrentSwapSafety concurrentSwapSafety = ConcurrentSwapSafety.RELIABLE;
		private StringEncoder stringEncoder = new UTF8Encoder();
		private BucketHasher bucketHasher = new XXHasher( seed );
		private FingerprintHasher fpHasher = new FixedHasher();

		/**
		 * <p>
		 * Instantiate a NanoCuckooFilter.Builder with the given capacity.
		 * </p>
		 * <p>
		 * The number of internal buckets will be (capacity / entries per bucket), scaled up to a power of 2.
		 * </p>
		 * 
		 * @param capacity
		 *            Desired filter capacity.
		 */
		public Builder( long capacity ) {

			if ( capacity <= 0 ) {
				throw new IllegalArgumentException( "Bucket Count must be positive" );
			}

			this.capacity = capacity;
		}

		/**
		 * <p>
		 * Build the filter using provided parameters. Will return null if sun.misc.Unsafe couldn't be obtained.
		 * </p>
		 *
		 * @return NanoCuckooFilter, or null.
		 */
		public NanoCuckooFilter build() {

			try {
				return new NanoCuckooFilter( entriesPerBucket, capacity, fpBits, maxKicks, seed, bucketHasher, fpHasher,
						concurrency, countingEnabled, smartInsertLoadFactor, stringEncoder, concurrentSwapSafety );
			} catch ( NoSuchFieldException | IllegalAccessException e ) {
				// Failed trying to obtain Unsafe. Shouldn't happen, return null if it does.
			}

			return null;
		}

		/**
		 * <p>
		 * Set entries per bucket.
		 * </p>
		 * <p>
		 * Impacts load factor and FPP.
		 * </p>
		 * <p>
		 * Must be a power of 2. Defaults to 4.
		 * </p>
		 *
		 * @param entriesPerBucket
		 *            Entries per bucket.
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
		 * <p>
		 * Set concurrent swap safety.
		 * </p>
		 * <p>
		 * The ConcurrentSwapSafety enum describes the levels available.
		 * </p>
		 * <p>
		 * Defaults to ConcurrentSwapSafety.RELIABLE.
		 * </p>
		 *
		 * @param concurrentSwapSafety
		 *            Concurrent swap safety.
		 * @return Updated Builder
		 */
		public Builder withConcurrentSwapSafety( ConcurrentSwapSafety concurrentSwapSafety ) {

			if ( concurrentSwapSafety == null ) {
				throw new IllegalArgumentException( "Concurrent Swap Safety must not be null" );
			}

			this.concurrentSwapSafety = concurrentSwapSafety;
			return this;
		}

		/**
		 * <p>
		 * Set SMART insert load factor.
		 * </p>
		 * <p>
		 * This is the maximum load factor to use for FAST inserts before switching to RELIABLE.
		 * </p>
		 * <p>
		 * Must be between 0 and 1. Defaults to 0.90 (90%).
		 * </p>
		 *
		 * @param smartInsertLoadFactor
		 *            SMART insert load factor.
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
		 * <p>
		 * Set fingerprint bits.
		 * </p>
		 * <p>
		 * More bits means lower FPP, but also more memory used.
		 * </p>
		 * <p>
		 * Must be from 1 to 32. Defaults to 8.
		 * </p>
		 *
		 * @param fpBits
		 *            Fingerprint bits.
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
		 * <p>
		 * Set max kicks.
		 * </p>
		 * <p>
		 * Higher will result in higher load factor before insert failure, but worse insert performance as max load is
		 * approached.
		 * </p>
		 * <p>
		 * Defaults to 400 (results in around 95% LF).
		 * </p>
		 *
		 * @param maxKicks
		 *            Max kicks.
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
		 * <p>
		 * Set random seed.
		 * </p>
		 * <p>
		 * Used when randomly swapping entries.
		 * </p>
		 * <p>
		 * Defaults to 0x48F7E28A.
		 * </p>
		 *
		 * @param seed
		 *            Random seed.
		 * @return Updated Builder
		 */
		public Builder withRandomSeed( int seed ) {

			this.seed = seed;
			return this;
		}

		/**
		 * <p>
		 * Set String encoder.
		 * </p>
		 * <p>
		 * Used by String insert/contains/count/delete.
		 * </p>
		 * <p>
		 * Defaults to UTF8Encoder.
		 * </p>
		 *
		 * @param stringEncoder
		 *            String encoder.
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
		 * <p>
		 * Set bucket hasher.
		 * </p>
		 * <p>
		 * Defaults to 64-bit XXHasher with a seed of 0x48F7E28A.
		 * </p>
		 *
		 * @param bucketHasher
		 *            Bucket hasher.
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
		 * <p>
		 * Set fingerprint hasher.
		 * </p>
		 * <p>
		 * Defaults to 64-bit FixedHasher.
		 * </p>
		 *
		 * @param fpHasher
		 *            Fingerprint hasher.
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
		 * <p>
		 * Set concurrency.
		 * </p>
		 * <p>
		 * Recommended to set to at least the number of Threads that will be inserting/deleting at once. A number
		 * ReentrantLocks will be allocated equal to this number.
		 * </p>
		 * <p>
		 * Capped by the number of buckets. Defaults to 64.
		 * </p>
		 *
		 * @param concurrency
		 *            Concurrency.
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
		 * <p>
		 * Set counting enabled.
		 * </p>
		 * <p>
		 * If counting is enabled, a single value can be inserted multiple times. Note that a value should not be
		 * inserted more times than (entries per bucket * 2 - 1), or filter failure can occur.
		 * <p>
		 * If counting is disabled, it is still possible for a value to end up in the filter multiple times, but an
		 * effort will be made to avoid it. Insert operations will return true if a duplicate is detected, as if the
		 * insert succeeded.
		 * </p>
		 * <p>
		 * Defaults to false.
		 * </p>
		 * 
		 * @param countingEnabled
		 *            Counting enabled.
		 * @return Updated Builder.
		 */
		public Builder withCountingEnabled( boolean countingEnabled ) {

			this.countingEnabled = countingEnabled;
			return this;
		}
	}
}
