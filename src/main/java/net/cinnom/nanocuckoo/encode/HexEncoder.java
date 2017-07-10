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
package net.cinnom.nanocuckoo.encode;

import java.io.Serializable;

/**
 * StringEncoder for encoding Hexadecimal Strings.
 */
public class HexEncoder implements StringEncoder, Serializable {

	private static final long serialVersionUID = 1L;

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
