package net.cinnom.nanocuckoo.performance;

import org.junit.Test;

import net.cinnom.nanocuckoo.metro.MetroHash64;
import net.cinnom.nanocuckoo.metro.UnsafeMetroHash64;
import net.jpountz.xxhash.XXHashFactory;

/**
 * Informal hash performance tests. Spoiler: Unsafe is much faster than safe.
 */
public class HashPerformanceIT {

	private final int size = 60;
	private final int iterations = 1_000_000_000;

	@Test
	public void smallMetroPerformanceTest() {

		final MetroHash64 metroHash = new MetroHash64();

		final byte[] values = new byte[size];
		for ( int i = 0; i < values.length; i++ ) {
			values[i] = (byte) i;
		}

		for ( int i = 1; i < iterations; i++ ) {
			metroHash.hash( values, 0, values.length, i );
		}
	}

	@Test
	public void smallMetroUnsafePerformanceTest() {

		final UnsafeMetroHash64 metroHash = new UnsafeMetroHash64();

		final byte[] values = new byte[size];
		for ( int i = 0; i < values.length; i++ ) {
			values[i] = (byte) i;
		}

		for ( int i = 1; i < iterations; i++ ) {
			metroHash.hash( values, 0, values.length, i );
		}
	}

	@Test
	public void smallXXPerformanceTest() {

		final XXHashFactory xxHash = XXHashFactory.safeInstance();

		final byte[] values = new byte[size];
		for ( int i = 0; i < values.length; i++ ) {
			values[i] = (byte) i;
		}

		for ( int i = 1; i < iterations; i++ ) {
			xxHash.hash64().hash( values, 0, values.length, i );
		}
	}

	@Test
	public void smallXXUnsafePerformanceTest() {

		final XXHashFactory xxHash = XXHashFactory.unsafeInstance();

		final byte[] values = new byte[size];
		for ( int i = 0; i < values.length; i++ ) {
			values[i] = (byte) i;
		}

		for ( int i = 1; i < iterations; i++ ) {
			xxHash.hash64().hash( values, 0, values.length, i );
		}
	}
}
