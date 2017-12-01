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
 * StringEncoder for encoding ASCII Strings. This encoder will only work with Strings containing ASCII characters.
 * <p>
 * Performance on this encoder is similar to {@link UTF16LEEncoder} for ASCII data.
 * </p>
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
