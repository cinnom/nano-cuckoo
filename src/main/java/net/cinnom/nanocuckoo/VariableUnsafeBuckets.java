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

/**
 * UnsafeBuckets type to be used when fingerprints are not 8, 16, or 32 bits. Performs a bunch of shifting and masking
 * to quickly and efficiently store fingerprints in 32-bit windows. Will be a bit slower compared to other buckets types
 * due to extra operations.
 */
final class VariableUnsafeBuckets extends UnsafeBuckets {

	private static final int BITS_PER_INT = 32;
	private static final int DIV_32 = 5;
	private static final int BYTES_PER_INT = 4;
	private static final int MULTI_4 = 2;
	private static final int MOD_32_MASK = 0x0000001F;

	private final int initClearMask;

	VariableUnsafeBuckets( int entries, long bucketCount, int fpBits, boolean countingDisabled ) {

		super( entries, bucketCount, fpBits, countingDisabled );

		int shift = BITS_PER_INT - fpBits;
		initClearMask = ( -1 >>> shift ) << shift;
	}

	// swap gets a special implementation here for more efficiency
	@Override
	int swap( int entry, long bucket, int value ) {

		long bucketBits = bucket * fpBits;

		long bucketByte = addresses[entry] + ( bucketBits >>> DIV_32 << MULTI_4 );
		int startBit = ( (int) ( bucketBits ) ) & MOD_32_MASK;

		int clearMask = initClearMask >>> startBit;

		int shift = BITS_PER_INT - ( startBit + fpBits );

		int getInt = unsafe.getIntVolatile( null, bucketByte );
		int putValue = value;

		getInt &= clearMask;

		startBit = 0;
		if ( shift > 0 ) {
			getInt >>>= shift;
			putValue = value << shift;
		} else if ( shift < 0 ) {
			startBit = -shift;
			getInt <<= startBit;
			putValue = value >>> startBit;
		}

		clearMask = ~clearMask;

		int originalInt;
		int putInt;
		do {
			originalInt = unsafe.getIntVolatile( null, bucketByte );

			putInt = ( originalInt & clearMask ) | putValue;

			// compareAndSwap loop is necessary since adjacent buckets could be concurrently modified
		} while ( !unsafe.compareAndSwapInt( null, bucketByte, originalInt, putInt ) );

		if ( startBit > 0 ) {

			bucketByte += BYTES_PER_INT;

			shift = BITS_PER_INT - startBit;

			clearMask = -1 >>> startBit;

			putValue = value << shift;

			do {
				originalInt = unsafe.getIntVolatile( null, bucketByte );

				putInt = ( originalInt & clearMask ) | putValue;

			} while ( !unsafe.compareAndSwapInt( null, bucketByte, originalInt, putInt ) );

			getInt |= originalInt >>> shift;
		}

		return getInt;
	}

	@Override
	int getValue( int entry, long bucket ) {

		long bucketBits = bucket * fpBits;

		long bucketByte = addresses[entry] + ( bucketBits >>> DIV_32 << MULTI_4 );
		int startBit = ( (int) ( bucketBits ) ) & MOD_32_MASK;

		int clearMask = initClearMask >>> startBit;

		int shift = BITS_PER_INT - ( startBit + fpBits );

		int getInt = unsafe.getIntVolatile( null, bucketByte );

		getInt &= clearMask;

		startBit = 0;

		getInt = shift > 0 ? getInt >>> shift : getInt;
		getInt = shift < 0 ? getInt << ( startBit = -shift ) : getInt;

		if ( startBit > 0 ) {

			int getInt2 = unsafe.getIntVolatile( null, bucketByte + BYTES_PER_INT );

			getInt2 >>>= ( BITS_PER_INT - startBit );

			getInt |= getInt2;
		}

		return getInt;
	}

	@Override
	void putValue( int entry, long bucket, int value ) {

		long bucketBits = bucket * fpBits;

		long bucketByte = addresses[entry] + ( bucketBits >>> DIV_32 << MULTI_4 );
		int startBit = ( (int) ( bucketBits ) ) & MOD_32_MASK;

		int clearMask = ~( initClearMask >>> startBit );

		int leftShift = BITS_PER_INT - ( startBit + fpBits );

		startBit = 0;

		int putValue = leftShift > 0 ? value << leftShift : value;
		putValue = leftShift < 0 ? value >>> ( startBit = -leftShift ) : putValue;

		int originalInt;
		int putInt;
		do {
			originalInt = unsafe.getIntVolatile( null, bucketByte );

			putInt = ( originalInt & clearMask ) | putValue;

		} while ( !unsafe.compareAndSwapInt( null, bucketByte, originalInt, putInt ) );

		if ( startBit > 0 ) {

			clearMask = -1 >>> startBit;

			putValue = value << ( BITS_PER_INT - startBit );

			bucketByte += BYTES_PER_INT;

			do {
				originalInt = unsafe.getIntVolatile( null, bucketByte );

				putInt = ( originalInt & clearMask ) | putValue;

			} while ( !unsafe.compareAndSwapInt( null, bucketByte, originalInt, putInt ) );
		}
	}

}
