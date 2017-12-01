/*
 * Copyright 2017 Randall Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
