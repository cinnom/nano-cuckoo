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
import net.cinnom.nanocuckoo.hash.BucketHasher;
import net.cinnom.nanocuckoo.hash.FingerprintHasher;
import net.cinnom.nanocuckoo.hash.FixedHasher;
import net.cinnom.nanocuckoo.hash.XXHasher;

/**
 * Provides serialization methods for optimizing serialized size.
 */
class Serialization {

	private static final byte CUSTOM_TYPE = 0;

	private static final byte UTF8_ENCODER_TYPE = 1;
	private static final byte UTF16LE_ENCODER_TYPE = 2;
	private static final byte ASCII_ENCODER_TYPE = 3;
	private static final byte HEX_ENCODER_TYPE = 4;

	private static final byte XXHASHER_BUCKET_HASHER_TYPE = 1;

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
		return CUSTOM_TYPE;
	}

	byte getBucketHasherType( final BucketHasher bucketHasher ) {

		if ( bucketHasher instanceof XXHasher ) {
			return XXHASHER_BUCKET_HASHER_TYPE;
		}
		return CUSTOM_TYPE;
	}

	byte getFingerprintHasherType( final FingerprintHasher fingerprintHasher ) {

		if ( fingerprintHasher instanceof FixedHasher ) {
			return FIXED_FP_HASHER_TYPE;
		}
		return CUSTOM_TYPE;
	}

	byte getRandomIntType() {

		return THREADLOCAL_RANDOM_INT_TYPE;
	}
}
