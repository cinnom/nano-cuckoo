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
 * UnsafeBuckets type to be used when fingerprints are exactly 16 bits. Makes calls to Unsafe Short methods.
 */
final class ShortUnsafeBuckets extends UnsafeBuckets {

	static final int FP_BITS = 16;
	private static final int BYTE_SHIFT = 1;

	ShortUnsafeBuckets( int entries, long bucketCount, boolean countingDisabled, long initialCount ) {

		super( entries, bucketCount, FP_BITS, countingDisabled, initialCount );
	}

	@Override
	int getValue( int entry, long bucket ) {

		return unsafe.getShortVolatile( null, addresses[entry] + ( bucket << BYTE_SHIFT ) ) & 0x0000FFFF;
	}

	@Override
	void putValue( int entry, long bucket, int value ) {

		unsafe.putShortVolatile( null, addresses[entry] + ( bucket << BYTE_SHIFT ), (short) value );
	}

}
