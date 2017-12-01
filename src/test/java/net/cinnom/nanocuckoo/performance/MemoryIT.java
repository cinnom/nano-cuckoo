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
