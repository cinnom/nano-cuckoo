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

/**
 * StringEncoder for encoding UTF-16 Little Endian Strings.
 */
public class UTF16LEEncoder implements StringEncoder {

	/**
	 * Encode a String into UTF-16 Little Endian bytes.
	 *
	 * @param data
	 *            String to encode.
	 * @return UTF-16 Little Endian bytes.
	 */
	@Override
	public byte[] encode( final String data ) {

		final int length = data.length();
		final byte resultBytes[] = new byte[length * 2];

		for ( int j = 0; j < length; j++ ) {

			int offset = j << 1;
			int nextOffset = offset + 1;
			char charAtJ = data.charAt( j );

			resultBytes[offset] = (byte) ( charAtJ & 0xFF );
			resultBytes[nextOffset] = (byte) ( charAtJ >> 8 );
		}

		return resultBytes;
	}
}
