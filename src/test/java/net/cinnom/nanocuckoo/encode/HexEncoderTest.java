package net.cinnom.nanocuckoo.encode;

import org.junit.Assert;
import org.junit.Test;

/**
 * HexEncoder tests
 */
public class HexEncoderTest {

	@Test
	public void encodeTest() {

		final HexEncoder hexEncoder = new HexEncoder();

		final String testStr = "123456789abCDEf0";

		final byte[] values = new byte[] { 18, 52, 86, 120, (byte) 154, (byte) 188, (byte) 222, (byte) 240 };

		int i = 0;
		for ( byte oneByte : hexEncoder.encode( testStr ) ) {
			Assert.assertEquals( values[i++], oneByte );
		}
	}

}
