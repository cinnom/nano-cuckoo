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
 * UnsafeBuckets type to be used when fingerprints are exactly 8 bits. Makes calls to Unsafe Byte methods.
 */
final class ByteUnsafeBuckets extends UnsafeBuckets {

	private static final int FP_BITS = 8;

	ByteUnsafeBuckets( int entries, long bucketCount, boolean countingDisabled ) {

		super( entries, bucketCount, FP_BITS, countingDisabled );
	}

	@Override
	int getValue( int entry, long bucket ) {

		return unsafe.getByteVolatile( null, addresses[entry] + bucket ) & 0x000000FF;
	}

	@Override
	void putValue( int entry, long bucket, int value ) {

		unsafe.putByteVolatile( null, addresses[entry] + bucket, (byte) value );
	}
}
