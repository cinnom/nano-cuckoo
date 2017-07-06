package org.cinnom.nanocuckoo.encode;

/**
 * StringEncoder for encoding ASCII Strings.
 */
public class ASCIIEncoder implements StringEncoder {

	/**
	 * Encode a String into ASCII bytes.
	 *
	 * @param data
	 *            String to encode.
	 * @return ASCII bytes.
	 */
	@Override
	public byte[] encode( final String data ) {

		final int length = data.length();
		final byte resultBytes[] = new byte[length];

		for ( int j = 0; j < length; j++ ) {

			resultBytes[j] = (byte) ( data.charAt( j ) & 0x7F );
		}

		return resultBytes;
	}
}
