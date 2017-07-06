package org.cinnom.nanocuckoo.encode;

import java.nio.charset.StandardCharsets;

/**
 * StringEncoder for encoding UTF-8 Strings.
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
