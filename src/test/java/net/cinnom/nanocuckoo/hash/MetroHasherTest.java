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
