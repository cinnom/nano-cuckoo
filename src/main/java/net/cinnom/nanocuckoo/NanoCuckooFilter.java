/*
 * Copyright 2017 Randall Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.cinnom.nanocuckoo;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.SplittableRandom;

import net.cinnom.nanocuckoo.encode.StringEncoder;
import net.cinnom.nanocuckoo.encode.UTF8Encoder;
import net.cinnom.nanocuckoo.hash.BucketHasher;
import net.cinnom.nanocuckoo.hash.FingerprintHasher;
import net.cinnom.nanocuckoo.hash.FixedHasher;
import net.cinnom.nanocuckoo.hash.XXHasher;

/**
 * <p>
 * Implements a Cuckoo Filter, as per "Cuckoo Filter: Practically Better Than Bloom" by Bin Fan, David G. Andersen,
 * Michael Kaminsky, and Michael D. Mitzenmacher.
 * </p>
 * <p>
 * This filter uses sun.misc.Unsafe to allocate native memory. Filter creation will fail if Unsafe can't be obtained, or
 * if memory can't be allocated.
 * </p>
 * <p>
 * Close this filter via its close() method, or a memory leak WILL occur.
 * </p>
 */
public class NanoCuckooFilter implements Serializable {

	private static final long serialVersionUID = 1L;

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

	private final KickedValues kickedValues;
	private final BucketLocker bucketLocker;
	private final Swapper swapper;

	NanoCuckooFilter( int fpBits, BucketHasher bucketHasher, FingerprintHasher fpHasher,
			StringEncoder stringEncoder, final KickedValues kickedValues, final UnsafeBuckets buckets,
			final BucketLocker bucketLocker, final Swapper swapper ) {

		this.kickedValues = kickedValues;
		this.buckets = buckets;
		this.stringEncoder = stringEncoder;
		this.bucketHasher = bucketHasher;
		this.fpHasher = fpHasher;
		this.fpBits = fpBits;
		this.bucketLocker = bucketLocker;
		this.swapper = swapper;

		// Set how many potential fingerprints we can locate in a bucket hash
		fpPerLong = BITS_PER_LONG / fpBits;

		// Set the mask that will pull fingerprint bits from the bucket hash
		fpMask = -1 >>> ( BITS_PER_INT - fpBits );
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
	 * Delete a specific number of occurrences of a given String in filter. Will use the initially set String encoder
	 * (UTF8Encoder by default).
	 *
	 * @param value
	 *            Value to delete.
	 * @param count
	 *            Number of occurrences to delete.
	 * @return Number of times value was deleted.
	 */
	public int delete( String value, int count ) {

		return delete( stringEncoder.encode( value ), count );
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
	public int delete( byte[] data, int count ) {

		return delete( bucketHasher.getHash( data ), count );
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
	public int delete( long hash, int count ) {

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
	 *            Value to delete.
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
	 * Get native memory bytes used by filter.
	 * 
	 * @return Native memory bytes used by filter.
	 */
	public long getMemoryUsageBytes() {

		return buckets.getMemoryUsageBytes();
	}

	/**
	 * Get filter maximum capacity.
	 * 
	 * @return Filter maximum capacity.
	 */
	public long getCapacity() {

		return buckets.getCapacity();
	}

	/**
	 * Get filter load factor (inserted count / capacity).
	 * 
	 * @return Filter load factor.
	 */
	public double getLoadFactor() {

		return (double) buckets.getInsertedCount() / buckets.getCapacity();
	}

	/**
	 * Double the number of entries per bucket. This will double overall capacity and memory usage, but will also
	 * roughly double the max FPP. Also slightly improves max load factor.
	 */
	public void expand() {

		bucketLocker.lockAllBuckets();
		buckets.expand();
		bucketLocker.unlockAllBuckets();
	}

	/**
	 * Close this filter. This needs to be called to deallocate native memory. Any attempts to use the filter after
	 * closing it will generally result in a NullPointerException.
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

	private void writeObject( ObjectOutputStream out ) throws IOException {

		bucketLocker.lockAllBuckets();
		out.defaultWriteObject();
		bucketLocker.unlockAllBuckets();
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
		 * Build the filter using provided parameters. Will throw a RuntimeException if sun.misc.Unsafe couldn't be
		 * obtained.
		 * </p>
		 *
		 * @return NanoCuckooFilter.
		 */
		public NanoCuckooFilter build() {

			boolean countingDisabled = !countingEnabled;

			final long bucketCount = (long) Math.ceil( (double) capacity / entriesPerBucket );

			UnsafeBuckets buckets;

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

			final KickedValues kickedValues = new KickedValues();
			final BucketLocker bucketLocker = new BucketLocker( concurrency, buckets.getBucketCount() );
			Swapper swapper;

			final SplittableRandom random = new SplittableRandom( seed );

			switch ( concurrentSwapSafety ) {

				case FAST:
					swapper = new FastSwapper( kickedValues, bucketLocker, buckets, fpHasher, maxKicks, seed, random );
					break;
				case SMART:
					Swapper fastSwapper = new FastSwapper( kickedValues, bucketLocker, buckets, fpHasher, maxKicks,
							seed, random );
					Swapper reliableSwapper = new ReliableSwapper( kickedValues, bucketLocker, buckets, fpHasher,
							maxKicks, seed, random );
					swapper = new SmartSwapper( fastSwapper, reliableSwapper, buckets, smartInsertLoadFactor );
					break;
				case RELIABLE:
				default:
					swapper = new ReliableSwapper( kickedValues, bucketLocker, buckets, fpHasher, maxKicks, seed,
							random );
					break;
			}

			return new NanoCuckooFilter( fpBits, bucketHasher, fpHasher, stringEncoder, kickedValues, buckets,
					bucketLocker, swapper );
		}

		/**
		 * <p>
		 * Set entries per bucket (AKA bucket size).
		 * </p>
		 * <p>
		 * Higher entries per bucket will give higher load factors, but also increases FPP.
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
		 * The {@link ConcurrentSwapSafety} enum describes the levels available.
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
