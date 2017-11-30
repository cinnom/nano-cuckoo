package net.cinnom.nanocuckoo.dumb;

import java.io.Serializable;

import net.cinnom.nanocuckoo.hash.BucketHasher;

public class DumbBucketHasher implements BucketHasher, Serializable {

	@Override
	public long getHash( final byte[] data ) {

		return data[0];
	}

	@Override
	public int getSeed() {

		return 0;
	}

}
