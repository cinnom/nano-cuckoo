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

import net.cinnom.nanocuckoo.encode.StringEncoder;
import net.cinnom.nanocuckoo.encode.UTF16LEEncoder;
import net.cinnom.nanocuckoo.hash.BucketHasher;
import net.cinnom.nanocuckoo.hash.FingerprintHasher;
import net.cinnom.nanocuckoo.hash.FixedHasher;
import net.cinnom.nanocuckoo.hash.XXHasher;
import net.cinnom.nanocuckoo.random.RandomInt;
import net.cinnom.nanocuckoo.random.WrappedThreadLocalRandom;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.ref.Cleaner;

/**
 * <p>
 * Implements a Cuckoo Filter, as per "Cuckoo Filter: Practically Better Than Bloom" by Bin Fan, David G. Andersen,
 * Michael Kaminsky, and Michael D. Mitzenmacher.
 * </p>
 * <p>
 * This filter uses sun.misc.Unsafe to allocate native memory. Filter creation will fail if Unsafe can't be obtained, or
 * if memory can't be allocated.
 * </p>
 */
public class NanoCuckooFilter implements Serializable {

	private static final long serialVersionUID = 2L;

	private static final int BITS_PER_INT = 32;
	private static final int BITS_PER_LONG = 64;

	private static final Cleaner CLEANER = Cleaner.create();

	private int fpBits;
	private transient int fpPerLong;
	private transient int fpMask;

	private transient UnsafeBuckets buckets;
	private transient StringEncoder stringEncoder;
	private transient BucketHasher bucketHasher;
	private transient FingerprintHasher fpHasher;

	private transient KickedValues kickedValues;
	private transient BucketLocker bucketLocker;
	private transient Swapper swapper;

	private transient Cleaner.Cleanable cleanable;

	NanoCuckooFilter( int fpBits, BucketHasher bucketHasher, FingerprintHasher fpHasher, StringEncoder stringEncoder,
			final KickedValues kickedValues, final UnsafeBuckets buckets, final BucketLocker bucketLocker,
			final Swapper swapper ) {

		this.kickedValues = kickedValues;
		this.buckets = buckets;
		this.stringEncoder = stringEncoder;
		this.bucketHasher = bucketHasher;
		this.fpHasher = fpHasher;
		this.fpBits = fpBits;
		this.bucketLocker = bucketLocker;
		this.swapper = swapper;

		initialize();
	}

	private void initialize() {

		// Set how many potential fingerprints we can locate in a bucket hash
		fpPerLong = BITS_PER_LONG / fpBits;

		// Set the mask that will pull fingerprint bits from the bucket hash
		fpMask = -1 >>> ( BITS_PER_INT - fpBits );

		cleanable = CLEANER.register( this, new Deallocator( buckets ) );
	}

	/**
	 * Insert a String into the filter. Will use the initially set String encoder (UTF8Encoder by default).
	 *
	 * @param value String to insert.
	 * @return True if value successfully inserted, false if filter is full.
	 */
	public boolean insert( String value ) {

		return insert( stringEncoder.encode( value ) );
	}

	/**
	 * Insert a byte array into the filter.
	 *
	 * @param data Data to insert.
	 * @return True if value successfully inserted, false if filter is full.
	 */
	public boolean insert( byte[] data ) {

		return insert( bucketHasher.getHash( data ) );
	}

	/**
	 * Insert a pre-hashed value into the filter.
	 *
	 * @param hash Hash to insert.
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

		return contains( bucketHasher.getHash( data ) );
	}

	/**
	 * Check if a given pre-hashed value has been inserted into the filter.
	 *
	 * @param hash Hash to check.
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
	 * @param value Value to count.
	 * @return Number of times value was previously inserted.
	 */
	public int count( String value ) {

		return count( stringEncoder.encode( value ) );
	}

	/**
	 * Count occurrences of given byte data in filter.
	 *
	 * @param data Data to count.
	 * @return Number of times data was previously inserted.
	 */
	public int count( byte[] data ) {

		return count( bucketHasher.getHash( data ) );
	}

	/**
	 * Count occurrences of a given pre-hashed value in filter.
	 *
	 * @param hash Hash to count.
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
	 * @param value Value to delete.
	 * @param count Number of occurrences to delete.
	 * @return Number of times value was deleted.
	 */
	public int delete( String value, int count ) {

		return delete( stringEncoder.encode( value ), count );
	}

	/**
	 * Delete a specific number of occurrences of given byte data in filter.
	 *
	 * @param data Data to delete.
	 * @param count Number of occurrences to delete.
	 * @return Number of times data was deleted.
	 */
	public int delete( byte[] data, int count ) {

		return delete( bucketHasher.getHash( data ), count );
	}

	/**
	 * Delete a specific number of occurrences of a given pre-hashed value in filter.
	 *
	 * @param hash Hash to delete.
	 * @param count Number of occurrences to delete.
	 * @return Number of times hash was deleted.
	 */
	public int delete( long hash, int count ) {

		try {
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
					kickedValues.unlock();
				}
			}
			return deletedCount;
		} finally {
			reinsertKickedFingerprint();
		}
	}

	/**
	 * Delete one occurrence of a given String in filter. Will use the initially set String encoder (UTF8Encoder by
	 * default).
	 *
	 * @param value Value to delete.
	 * @return True if value was deleted, false if not.
	 */
	public boolean delete( String value ) {

		return delete( stringEncoder.encode( value ) );
	}

	/**
	 * Delete one occurrence of given byte data in filter.
	 *
	 * @param data Data to delete.
	 * @return True if data was deleted, false if not.
	 */
	public boolean delete( byte[] data ) {

		return delete( bucketHasher.getHash( data ) );
	}

	/**
	 * Delete one occurrence of a given pre-hashed value in filter.
	 *
	 * @param hash Hash to delete.
	 * @return True if hash was deleted, false if not.
	 */
	public boolean delete( long hash ) {

		try {
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
				kickedValues.unlock();
			}
		} finally {
			reinsertKickedFingerprint();
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

		return buckets.getTotalCapacity();
	}

	/**
	 * Get filter load factor (inserted count / capacity).
	 *
	 * @return Filter load factor.
	 */
	public double getLoadFactor() {

		return (double) buckets.getInsertedCount() / buckets.getTotalCapacity();
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
	 * Close this filter. This can to be called to immediately deallocate native memory. Otherwise, the memory will be
	 * freed when the GC gets around to it. Any attempts to use the filter after closing it will generally result in a
	 * NullPointerException.
	 */
	public void close() {

		cleanable.clean();
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

			// Slight performance improvement for concurrent deletes - only lock if we need to
			kickedValues.lock();

			if ( !kickedValues.isClear() ) {

				int kickedFingerprint = kickedValues.getKickedFingerprint();
				long kickedBucket = kickedValues.getKickedBucket();
				kickedValues.clear();
				buckets.decrementInsertedCount();

				insertFingerprint( kickedFingerprint, kickedBucket );
			}

			kickedValues.unlock();
		}
	}

	private void readObject( ObjectInputStream in ) throws IOException, ClassNotFoundException {

		loadFilter( in, this );
	}

	private void writeObject( ObjectOutputStream out ) throws IOException {

		saveFilter( out );
	}

	/**
	 * Create a new filter from the given ObjectInputStream. To be used in conjunction with
	 * {@link NanoCuckooFilter#saveFilter(ObjectOutputStream)}.
	 *
	 * @param in ObjectInputStream to read filter data from.
	 * @return Loaded NanoCuckooFilter.
	 * @throws IOException Thrown by ObjectInputStream.
	 * @throws ClassNotFoundException Thrown by ObjectInputStream.
	 */
	public static NanoCuckooFilter loadFilter( final ObjectInputStream in ) throws IOException, ClassNotFoundException {

		return loadFilter( in, null );
	}

	private static NanoCuckooFilter loadFilter( final ObjectInputStream in, final NanoCuckooFilter filter )
			throws IOException, ClassNotFoundException {

		final Serialization serialization = new Serialization();

		// Create buckets
		final int fpBits = in.readInt();
		final int entriesPerBucket = in.readInt();
		final long bucketCount = in.readLong();
		final boolean countingDisabled = in.readBoolean();
		final UnsafeBuckets buckets = UnsafeBuckets
				.createBuckets( fpBits, entriesPerBucket, bucketCount, countingDisabled, in.readLong() );
		buckets.readMemory( in );

		// Create kicked values
		final KickedValues kickedValues = new KickedValues();
		kickedValues.setKickedFingerprint( in.readInt() );
		kickedValues.setKickedBucket( in.readLong() );

		// Create bucket locker
		final int concurrency = in.readInt();
		final BucketLocker bucketLocker = new BucketLocker( concurrency, bucketCount );

		// Create swapper
		int maxKicks = in.readInt();
		// Create random int provider
		final byte randomIntType = in.readByte();
		RandomInt randomInt;
		if ( randomIntType > 0 ) {
			randomInt = serialization.createRandomInt( randomIntType );
		} else {
			randomInt = (RandomInt) in.readObject();
		}

		// Create string encoder
		final byte stringEncoderType = in.readByte();
		StringEncoder stringEncoder;
		if ( stringEncoderType > 0 ) {
			stringEncoder = serialization.createStringEncoder( stringEncoderType );
		} else {
			stringEncoder = (StringEncoder) in.readObject();
		}

		// Create bucket hasher
		final byte bucketHasherType = in.readByte();
		BucketHasher bucketHasher;
		if ( bucketHasherType > 0 ) {
			bucketHasher = serialization.createBucketHasher( bucketHasherType, in.readInt() );
		} else {
			bucketHasher = (BucketHasher) in.readObject();
		}

		// Create fingerprint hasher
		final byte fpHasherType = in.readByte();
		FingerprintHasher fpHasher;
		if ( fpHasherType > 0 ) {
			fpHasher = serialization.createFingerprintHasher( fpHasherType );
		} else {
			fpHasher = (FingerprintHasher) in.readObject();
		}

		final Swapper swapper = new Swapper( kickedValues, bucketLocker, buckets, fpHasher, maxKicks, randomInt );

		if ( filter == null ) {
			return new NanoCuckooFilter( fpBits, bucketHasher, fpHasher, stringEncoder, kickedValues, buckets,
					bucketLocker, swapper );
		} else {
			filter.fpBits = fpBits;
			filter.buckets = buckets;
			filter.kickedValues = kickedValues;
			filter.bucketLocker = bucketLocker;
			filter.stringEncoder = stringEncoder;
			filter.bucketHasher = bucketHasher;
			filter.fpHasher = fpHasher;
			filter.swapper = swapper;
			filter.initialize();
			return filter;
		}
	}

	/**
	 * Save filter data to the given ObjectOutputStream. To be used in conjunction with
	 * {@link NanoCuckooFilter#loadFilter(ObjectInputStream)}.
	 *
	 * @param out ObjectOutputStream to write filter data to.
	 * @throws IOException Thrown by ObjectOutputStream.
	 */
	public void saveFilter( final ObjectOutputStream out ) throws IOException {

		bucketLocker.lockAllBuckets();

		final Serialization serialization = new Serialization();

		// Write bucket values
		out.writeInt( fpBits );
		out.writeInt( buckets.getEntriesPerBucket() );
		out.writeLong( buckets.getBucketCount() );
		out.writeBoolean( buckets.isCountingDisabled() );
		out.writeLong( buckets.getInsertedCount() );
		buckets.writeMemory( out );

		// Write kicked values
		out.writeInt( kickedValues.getKickedFingerprint() );
		out.writeLong( kickedValues.getKickedBucket() );

		// Write bucket locker values
		out.writeInt( bucketLocker.getConcurrency() );

		// Write swapper values
		out.writeInt( swapper.getMaxKicks() );
		// Write random int provider
		final byte randomIntType = serialization.getRandomIntType( swapper.getRandomInt() );
		out.writeByte( randomIntType );
		if ( serialization.isCustomType( randomIntType ) ) {
			out.writeObject( swapper.getRandomInt() );
		}

		// Write string encoder
		final byte stringEncoderType = serialization.getStringEncoderType( stringEncoder );
		out.writeByte( stringEncoderType );
		if ( serialization.isCustomType( stringEncoderType ) ) {
			out.writeObject( stringEncoder );
		}

		// Write bucket hasher
		final byte bucketHasherType = serialization.getBucketHasherType( bucketHasher );
		out.writeByte( bucketHasherType );
		if ( serialization.isCustomType( bucketHasherType ) ) {
			out.writeObject( bucketHasher );
		} else {
			out.writeInt( bucketHasher.getSeed() );
		}

		// Write fingerprint hasher
		final byte fpHasherType = serialization.getFingerprintHasherType( fpHasher );
		out.writeByte( fpHasherType );
		if ( serialization.isCustomType( fpHasherType ) ) {
			out.writeObject( fpHasher );
		}

		bucketLocker.unlockAllBuckets();
	}

	/**
	 * Write the filter's internal memory to the given OutputStream. To be used in conjunction with
	 * {@link NanoCuckooFilter#readMemory(InputStream)}.
	 *
	 * @param outputStream Output stream to write memory to.
	 * @throws IOException Thrown by OutputStream.
	 */
	public void writeMemory( OutputStream outputStream ) throws IOException {

		bucketLocker.lockAllBuckets();
		buckets.writeMemory( outputStream );
		bucketLocker.unlockAllBuckets();
	}

	/**
	 * Overwrite the internal filter memory. This should only be used in conjunction with
	 * {@link NanoCuckooFilter#writeMemory(OutputStream)}, and the filters' capacity, entries per bucket, and
	 * fingerprint size should match.
	 *
	 * @param inputStream Input stream to read memory from.
	 * @throws IOException Thrown by InputStream.
	 */
	public void readMemory( InputStream inputStream ) throws IOException {

		bucketLocker.lockAllBuckets();
		buckets.readMemory( inputStream );
		bucketLocker.unlockAllBuckets();
	}

	/**
	 * Closes UnsafeBuckets.
	 */
	static class Deallocator implements Runnable {

		private final UnsafeBuckets buckets;

		Deallocator( final UnsafeBuckets buckets ) {

			this.buckets = buckets;
		}

		@Override public void run() {

			buckets.close();
		}
	}

	/**
	 * Builder for NanoCuckooFilter.
	 */
	public static class Builder {

		private static final int POS_INT = 0x7FFFFFFF;
		public static final int DEFAULT_SEED = 0x48F7E28A;
		private final long capacity;
		private int entriesPerBucket = 4;
		private int fpBits = 8;
		private int maxKicks = 400;
		private int concurrency = 64;
		private boolean countingEnabled = false;
		private StringEncoder stringEncoder = new UTF16LEEncoder();
		private BucketHasher bucketHasher = new XXHasher( DEFAULT_SEED );
		private FingerprintHasher fpHasher = new FixedHasher();
		private RandomInt randomInt = new WrappedThreadLocalRandom();

		/**
		 * <p>
		 * Instantiate a NanoCuckooFilter.Builder with the given capacity.
		 * </p>
		 * <p>
		 * The number of internal buckets will be (capacity / entries per bucket), scaled up to a power of 2.
		 * </p>
		 *
		 * @param capacity Desired filter capacity.
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

			final UnsafeBuckets buckets = UnsafeBuckets
					.createBuckets( fpBits, entriesPerBucket, bucketCount, countingDisabled, 0 );
			final KickedValues kickedValues = new KickedValues();
			final BucketLocker bucketLocker = new BucketLocker( concurrency, buckets.getBucketCount() );
			final Swapper swapper = new Swapper( kickedValues, bucketLocker, buckets, fpHasher, maxKicks, randomInt );

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
		 * <p>
		 * Set String encoder.
		 * </p>
		 * <p>
		 * Used by String insert/contains/count/delete.
		 * </p>
		 * <p>
		 * Defaults to {@link UTF16LEEncoder}.
		 * </p>
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
		 * <p>
		 * Set bucket hasher.
		 * </p>
		 * <p>
		 * Defaults to {@link XXHasher} with a seed of {@value #DEFAULT_SEED}.
		 * </p>
		 *
		 * @param bucketHasher Bucket hasher.
		 * @return Updated Builder
		 */
		public Builder withBucketHasher( BucketHasher bucketHasher ) {

			if ( bucketHasher == null ) {
				throw new IllegalArgumentException( "BucketHasher must not be null" );
			}

			this.bucketHasher = bucketHasher;
			return this;
		}

		/**
		 * <p>
		 * Set fingerprint hasher.
		 * </p>
		 * <p>
		 * Defaults to {@link FixedHasher}.
		 * </p>
		 *
		 * @param fpHasher Fingerprint hasher.
		 * @return Updated Builder
		 */
		public Builder withFingerprintHasher( FingerprintHasher fpHasher ) {

			if ( fpHasher == null ) {
				throw new IllegalArgumentException( "FingerprintHasher must not be null" );
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
		 * @param countingEnabled Counting enabled.
		 * @return Updated Builder.
		 */
		public Builder withCountingEnabled( boolean countingEnabled ) {

			this.countingEnabled = countingEnabled;
			return this;
		}

		/**
		 * <p>
		 * Set random int provider.
		 * </p>
		 * <p>
		 * Defaults to {@link WrappedThreadLocalRandom}.
		 * </p>
		 *
		 * @param randomInt Random int provider.
		 * @return Updated Builder
		 */
		public Builder withRandomInt( RandomInt randomInt ) {

			if ( randomInt == null ) {
				throw new IllegalArgumentException( "RandomInt must not be null" );
			}

			this.randomInt = randomInt;
			return this;
		}
	}
}
