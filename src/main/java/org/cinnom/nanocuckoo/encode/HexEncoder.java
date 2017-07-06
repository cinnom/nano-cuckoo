package org.cinnom.nanocuckoo.encode;

/**
 * StringEncoder for encoding Hexadecimal Strings.
 */
public class HexEncoder implements StringEncoder {

	/**
	 * Encode a String into Hexadecimal value bytes. This method assumes that the input is a valid Hexadecimal String;
	 * no validation is performed on the input.
	 * 
	 * @param data
	 *            String to encode.
	 * @return Hex value bytes.
	 */
	@Override
	public byte[] encode( final String data ) {

		final int length = data.length() >> 1;
		final byte resultBytes[] = new byte[length];

		for ( int j = 0; j < length; j++ ) {

			int offset = j << 1;
			int nextOffset = offset + 1;

			resultBytes[j] = (byte) ( ( getHexValue( data.charAt( offset ) ) << 4 )
					+ getHexValue( data.charAt( nextOffset ) ) );
		}

		return resultBytes;
	}

	private static byte getHexValue( char c ) {

		byte value = (byte) c;
		value -= 48;
		if ( value > 15 ) {
			value -= 7;
			if ( value > 15 ) {
				value -= 32;
			}
		}
		return value;
	}
}
