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

import net.cinnom.nanocuckoo.encode.ASCIIEncoder;
import net.cinnom.nanocuckoo.encode.HexEncoder;
import net.cinnom.nanocuckoo.encode.StringEncoder;
import net.cinnom.nanocuckoo.encode.UTF16LEEncoder;
import net.cinnom.nanocuckoo.encode.UTF8Encoder;
import net.cinnom.nanocuckoo.encode.UnsafeEncoder;
import net.cinnom.nanocuckoo.hash.BucketHasher;
import net.cinnom.nanocuckoo.hash.FingerprintHasher;
import net.cinnom.nanocuckoo.hash.FixedHasher;
import net.cinnom.nanocuckoo.hash.MetroHasher;
import net.cinnom.nanocuckoo.hash.XXHasher;
import net.cinnom.nanocuckoo.random.RandomInt;
import net.cinnom.nanocuckoo.random.WrappedThreadLocalRandom;

/**
 * Provides serialization methods for optimizing serialized size.
 */
class Serialization {

	private static final byte CUSTOM_TYPE = 0;

	private static final byte UTF8_ENCODER_TYPE = 1;
	private static final byte UTF16LE_ENCODER_TYPE = 2;
	private static final byte ASCII_ENCODER_TYPE = 3;
	private static final byte HEX_ENCODER_TYPE = 4;
	private static final byte UNSAFE_ENCODER_TYPE = 5;

	private static final byte XXHASHER_BUCKET_HASHER_TYPE = 1;
	private static final byte METROHASHER_BUCKET_HASHER_TYPE = 2;

	private static final byte FIXED_FP_HASHER_TYPE = 1;

	private static final byte THREADLOCAL_RANDOM_INT_TYPE = 1;

	boolean isCustomType( byte type ) {

		return type == CUSTOM_TYPE;
	}

	byte getStringEncoderType( final StringEncoder stringEncoder ) {

		if ( stringEncoder instanceof UTF8Encoder ) {
			return UTF8_ENCODER_TYPE;
		}
		if ( stringEncoder instanceof UTF16LEEncoder ) {
			return UTF16LE_ENCODER_TYPE;
		}
		if ( stringEncoder instanceof ASCIIEncoder ) {
			return ASCII_ENCODER_TYPE;
		}
		if ( stringEncoder instanceof HexEncoder ) {
			return HEX_ENCODER_TYPE;
		}
		if ( stringEncoder instanceof UnsafeEncoder ) {
			return UNSAFE_ENCODER_TYPE;
		}
		return CUSTOM_TYPE;
	}

	StringEncoder createStringEncoder( final byte type ) {

		switch ( type ) {
			case ASCII_ENCODER_TYPE:
				return new ASCIIEncoder();
			case HEX_ENCODER_TYPE:
				return new HexEncoder();
			case UTF8_ENCODER_TYPE:
				return new UTF8Encoder();
			case UNSAFE_ENCODER_TYPE:
				return new UnsafeEncoder();
			case UTF16LE_ENCODER_TYPE:
			default:
				return new UTF16LEEncoder();
		}
	}

	byte getBucketHasherType( final BucketHasher bucketHasher ) {

		if ( bucketHasher instanceof XXHasher ) {
			return XXHASHER_BUCKET_HASHER_TYPE;
		}
		if ( bucketHasher instanceof MetroHasher ) {
			return METROHASHER_BUCKET_HASHER_TYPE;
		}
		return CUSTOM_TYPE;
	}

	BucketHasher createBucketHasher( final byte type, final int seed ) {

		switch ( type ) {
			case METROHASHER_BUCKET_HASHER_TYPE:
				return new MetroHasher( seed );
			case XXHASHER_BUCKET_HASHER_TYPE:
			default:
				return new XXHasher( seed );
		}
	}

	byte getFingerprintHasherType( final FingerprintHasher fingerprintHasher ) {

		if ( fingerprintHasher instanceof FixedHasher ) {
			return FIXED_FP_HASHER_TYPE;
		}
		return CUSTOM_TYPE;
	}

	FingerprintHasher createFingerprintHasher( final byte type ) {

		switch ( type ) {
			case FIXED_FP_HASHER_TYPE:
			default:
				return new FixedHasher();
		}
	}

	byte getRandomIntType( final RandomInt randomInt ) {

		if ( randomInt instanceof WrappedThreadLocalRandom ) {
			return THREADLOCAL_RANDOM_INT_TYPE;
		}
		return CUSTOM_TYPE;
	}

	RandomInt createRandomInt( final byte type ) {

		switch ( type ) {
			case THREADLOCAL_RANDOM_INT_TYPE:
			default:
				return new WrappedThreadLocalRandom();
		}
	}
}
