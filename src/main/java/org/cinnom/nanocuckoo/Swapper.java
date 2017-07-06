package org.cinnom.nanocuckoo;

/**
 * Bucket swapper. Tries to swap values around until a successful insert. Will swap up to specified MaxKicks before
 * failing.
 */
interface Swapper {

	boolean swap( int fingerprint, long bucket );

}
