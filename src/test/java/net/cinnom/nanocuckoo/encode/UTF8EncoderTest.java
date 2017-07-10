package net.cinnom.nanocuckoo.encode;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * UTF8Encoder tests
 */
public class UTF8EncoderTest {

	@Test
	public void encodeTest() {

		final UTF8Encoder utf8Encoder = new UTF8Encoder();

		final String testStr = "ve09jw@$%THafw09\uD83D\uDD8F";

		final byte[] values = testStr.getBytes( StandardCharsets.UTF_8 );;

		int i = 0;
		for ( byte oneByte : utf8Encoder.encode( testStr ) ) {
			Assert.assertEquals( values[i++], oneByte );
		}
	}

}
