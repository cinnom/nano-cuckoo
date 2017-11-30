package net.cinnom.nanocuckoo.metro;

import org.junit.Assert;
import org.junit.Test;

public class UnsafeMetroHash64Test {

	private final int size = 127;

	@Test
	public void getHashTest() {

		final int seed = 0x47A7A28A;

		final UnsafeMetroHash64 metroHasher = new UnsafeMetroHash64();

		final byte[] values = new byte[size];
		for ( int i = 0; i < values.length; i++ ) {
			values[i] = (byte) i;
		}

		Assert.assertEquals( -6601481954667490441L, metroHasher.hash( values, 0, values.length, seed ) );
	}

	@Test
	public void getHashTestDifferentSeed() {

		final int seed = 0x47F7E28A;

		final UnsafeMetroHash64 metroHasher = new UnsafeMetroHash64();

		final byte[] values = new byte[size];
		for ( int i = 0; i < values.length; i++ ) {
			values[i] = (byte) i;
		}

		Assert.assertEquals( 5100246155154649873L, metroHasher.hash( values, 0, values.length, seed ) );
	}
}
