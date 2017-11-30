package net.cinnom.nanocuckoo;

import net.cinnom.nanocuckoo.encode.ASCIIEncoder;
import net.cinnom.nanocuckoo.encode.HexEncoder;
import net.cinnom.nanocuckoo.encode.StringEncoder;
import net.cinnom.nanocuckoo.encode.UTF16LEEncoder;
import net.cinnom.nanocuckoo.encode.UTF8Encoder;
import net.cinnom.nanocuckoo.hash.BucketHasher;
import net.cinnom.nanocuckoo.hash.FingerprintHasher;
import net.cinnom.nanocuckoo.hash.FixedHasher;
import net.cinnom.nanocuckoo.hash.MetroHasher;
import net.cinnom.nanocuckoo.hash.XXHasher;
import net.cinnom.nanocuckoo.random.RandomInt;
import net.cinnom.nanocuckoo.random.WrappedThreadLocalRandom;

class Serialization {

	static final byte CUSTOM_ENCODER_TYPE = 0;
	private static final byte UTF8_ENCODER_TYPE = 1;
	private static final byte UTF16LE_ENCODER_TYPE = 2;
	private static final byte ASCII_ENCODER_TYPE = 3;
	private static final byte HEX_ENCODER_TYPE = 4;

	static final byte CUSTOM_BUCKET_HASHER_TYPE = 0;
	private static final byte XXHASHER_BUCKET_HASHER_TYPE = 1;
	private static final byte METROHASHER_BUCKET_HASHER_TYPE = 2;

	static final byte CUSTOM_FP_HASHER_TYPE = 0;
	private static final byte FIXED_FP_HASHER_TYPE = 1;

	static final byte CUSTOM_RANDOM_INT_TYPE = 0;
	private static final byte THREADLOCAL_RANDOM_INT_TYPE = 1;

	static byte getStringEncoderType( final StringEncoder stringEncoder ) {

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
		return CUSTOM_ENCODER_TYPE;
	}

	static StringEncoder createStringEncoder( final byte type ) {

		switch ( type ) {
			case ASCII_ENCODER_TYPE:
				return new ASCIIEncoder();
			case HEX_ENCODER_TYPE:
				return new HexEncoder();
			case UTF16LE_ENCODER_TYPE:
				return new UTF16LEEncoder();
			case UTF8_ENCODER_TYPE:
			default:
				return new UTF8Encoder();
		}
	}

	static byte getBucketHasherType( final BucketHasher bucketHasher ) {

		if ( bucketHasher instanceof XXHasher ) {
			return XXHASHER_BUCKET_HASHER_TYPE;
		}
		if ( bucketHasher instanceof MetroHasher ) {
			return METROHASHER_BUCKET_HASHER_TYPE;
		}
		return CUSTOM_BUCKET_HASHER_TYPE;
	}

	static BucketHasher createBucketHasher( final byte type, final int seed ) {

		switch ( type ) {
			case METROHASHER_BUCKET_HASHER_TYPE:
				return new MetroHasher( seed );
			case XXHASHER_BUCKET_HASHER_TYPE:
			default:
				return new XXHasher( seed );
		}
	}

	static byte getFingerprintHasherType( final FingerprintHasher fingerprintHasher ) {

		if ( fingerprintHasher instanceof FixedHasher ) {
			return FIXED_FP_HASHER_TYPE;
		}
		return CUSTOM_FP_HASHER_TYPE;
	}

	static FingerprintHasher createFingerprintHasher( final byte type ) {

		switch ( type ) {
			case FIXED_FP_HASHER_TYPE:
			default:
				return new FixedHasher();
		}
	}

	static byte getRandomIntType( final RandomInt randomInt ) {

		if ( randomInt instanceof WrappedThreadLocalRandom ) {
			return THREADLOCAL_RANDOM_INT_TYPE;
		}
		return CUSTOM_RANDOM_INT_TYPE;
	}

	static RandomInt createRandomInt( final byte type ) {

		switch ( type ) {
			case THREADLOCAL_RANDOM_INT_TYPE:
			default:
				return new WrappedThreadLocalRandom();
		}
	}
}
