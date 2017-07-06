package org.cinnom.nanocuckoo.encode;

/**
 * Provides functionality for encoding a String into a byte array.
 */
public interface StringEncoder {

	/**
	 * Encodes a String into a byte array.
	 * 
	 * @param data
	 *            String to encode.
	 * @return Byte array.
	 */
	byte[] encode( final String data );
}
