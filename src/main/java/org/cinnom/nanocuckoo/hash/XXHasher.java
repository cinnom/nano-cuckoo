package org.cinnom.nanocuckoo.hash;

import net.jpountz.xxhash.XXHashFactory;

/**
 * Created by rjones on 6/22/17.
 */
public final class XXHasher implements FingerprintHasher, BucketHasher {

	private final XXHashFactory factory = XXHashFactory.fastestInstance();

	private int seed;

	public XXHasher( int seed ) {
		this.seed = seed;
	}

	@Override
	public long getHash( final byte[] data ) {

		return factory.hash64().hash( data, 0, data.length, seed );
	}

	@Override
	public long getHash( final int value ) {

		// Transform int to BE byte array
		final byte[] buf = new byte[] { (byte) ( value >>> 24 ), (byte) ( value >>> 16 ), (byte) ( value >>> 8 ),
				(byte) value };

		return factory.hash64().hash( buf, 0, 4, seed );
	}

}
