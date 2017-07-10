package net.cinnom.nanocuckoo.hash;

import org.junit.Assert;
import org.junit.Test;

/**
 * XXHasher tests.
 */
public class XXHasherTest {

	@Test
	public void getHashTest() {

		final int seed = 0x48F7E28A;

		final XXHasher xxHasher = new XXHasher( seed );

		final byte[] values = new byte[1000];
		for(int i = 0; i < 1000; i++) {
			values[i] = (byte) i;
		}

		Assert.assertEquals( -4073436676363075178L, xxHasher.getHash( values ));
	}

	@Test
	public void getHashTestDifferentSeed() {

		final int seed = 0x47F7E28A;

		final XXHasher xxHasher = new XXHasher( seed );

		final byte[] values = new byte[1000];
		for(int i = 0; i < 1000; i++) {
			values[i] = (byte) i;
		}

		Assert.assertEquals( -6925839588689899575L, xxHasher.getHash( values ));
	}
}
