package net.cinnom.nanocuckoo.hash;

import org.junit.Assert;
import org.junit.Test;

/**
 * FixedHasher tests.
 */
public class FixedHasherTest {

	@Test
	public void getHashTest() {

		final FixedHasher fixedHasher = new FixedHasher();

		final int testValue = 127;

		Assert.assertEquals( testValue * 0xC4CEB9FE1A85EC53L, fixedHasher.getHash( testValue ));
	}
}
