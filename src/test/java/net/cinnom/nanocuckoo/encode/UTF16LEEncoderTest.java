package net.cinnom.nanocuckoo.encode;

import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

/**
 * UTF16LEEncoder tests
 */
public class UTF16LEEncoderTest {

	@Test
	public void encodeTest() {

		final UTF16LEEncoder utf16leEncoder = new UTF16LEEncoder();

		final String testStr = "ve09jw@$%THafw09\uD83D\uDD8F";

		final byte[] values = testStr.getBytes( StandardCharsets.UTF_16LE );

		int i = 0;
		for ( byte oneByte : utf16leEncoder.encode( testStr ) ) {
			Assert.assertEquals( values[i++], oneByte );
		}
	}

}
