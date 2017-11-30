package net.cinnom.nanocuckoo;

import net.cinnom.nanocuckoo.encode.UTF16LEEncoder;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class UTF16LEEncoderTest {

	@Test
	public void test() {

		final UTF16LEEncoder utf16LEEncoder = new UTF16LEEncoder();

		Assert.assertEquals("[116, 0, 101, 0, 115, 0, 116, 0]", Arrays.toString(utf16LEEncoder.encode( "test" )));
	}
}
