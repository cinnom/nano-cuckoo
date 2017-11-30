package net.cinnom.nanocuckoo.concurrency;

import org.junit.Test;

import net.cinnom.nanocuckoo.NanoCuckooFilter;

/**
 * Memory integration tests.
 */
public class MemoryIT {

	/**
	 * Should cap out around 865~ MB.
	 */
	@Test
	public void cleanerTest() throws InterruptedException {

		final int capacity = 10_000_000;

		for ( int i = 0; i < 5_000; i++ ) {
			final NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( capacity ).withFingerprintBits( 8 )
					.build();
			if ( i % 50 == 0 ) {
				System.gc();
			}
		}
	}
}
