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

import java.nio.charset.StandardCharsets;

/**
 * StringEncoder for encoding UTF-8 Strings. This encoder should work for any String.
 * <p>
 * Performance on this encoder is generally worse than any other encoders; {@link UTF16LEEncoder} should be used
 * instead.
 * </p>
 */
public class UTF8Encoder implements StringEncoder {

	/**
	 * Encode a String into UTF-8 bytes.
	 *
	 * @param data
	 *            String to encode.
	 * @return UTF-8 bytes.
	 */
	@Override
	public byte[] encode( final String data ) {

		return data.getBytes( StandardCharsets.UTF_8 );
	}
}
