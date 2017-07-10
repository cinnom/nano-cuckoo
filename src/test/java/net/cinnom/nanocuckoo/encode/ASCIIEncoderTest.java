package net.cinnom.nanocuckoo.encode;

import org.junit.Assert;
import org.junit.Test;

/**
 * ASCIIEncoder tests
 */
public class ASCIIEncoderTest {

	@Test
	public void encodeTest() {

		final ASCIIEncoder asciiEncoder = new ASCIIEncoder();

		// All ASCII characters from 32 to 126
		final String testStr = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

		int i = 32;
		for(byte oneByte : asciiEncoder.encode( testStr ) ) {
			Assert.assertEquals(i++, oneByte);
		}
	}

}
