package net.cinnom.nanocuckoo.dumb;

import java.io.Serializable;

import net.cinnom.nanocuckoo.hash.FingerprintHasher;

public class DumbFingerprintHasher implements FingerprintHasher, Serializable {

	@Override
	public long getHash( final int value ) {

		return value;
	}
}
