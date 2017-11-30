package net.cinnom.nanocuckoo.hash;

import net.cinnom.nanocuckoo.NanoCuckooFilter;
import org.junit.Assert;
import org.junit.Test;

/**
 * MetroHash tests
 */
public class MetroHasherTest {

	private final int size = 127;

	@Test
	public void getHashTest() {

		final MetroHasher metroHasher = new MetroHasher( NanoCuckooFilter.Builder.DEFAULT_SEED );

		final byte[] values = new byte[size];
		for ( int i = 0; i < values.length; i++ ) {
			values[i] = (byte) i;
		}

		Assert.assertEquals( NanoCuckooFilter.Builder.DEFAULT_SEED, metroHasher.getSeed() );
		Assert.assertEquals( -4604957212005795163L, metroHasher.getHash( values ) );
	}

	@Test
	public void getHashTestDifferentSeed() {

		final int seed = 0x47F7E28A;

		final MetroHasher metroHasher = new MetroHasher( seed );

		final byte[] values = new byte[size];
		for ( int i = 0; i < values.length; i++ ) {
			values[i] = (byte) i;
		}

		Assert.assertEquals( seed, metroHasher.getSeed() );
		Assert.assertEquals( 5100246155154649873L, metroHasher.getHash( values ) );
	}
}
