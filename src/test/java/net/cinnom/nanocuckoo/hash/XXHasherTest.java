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
 * XXHasher tests.
 */
public class XXHasherTest {

	@Test
	public void getHashTest() {

		final XXHasher xxHasher = new XXHasher( NanoCuckooFilter.Builder.DEFAULT_SEED );

		final byte[] values = new byte[1000];
		for ( int i = 0; i < 1000; i++ ) {
			values[i] = (byte) i;
		}

		Assert.assertEquals( -4073436676363075178L, xxHasher.getHash( values ) );
	}

	@Test
	public void getHashTestDifferentSeed() {

		final int seed = 0x47F7E28A;

		final XXHasher xxHasher = new XXHasher( seed );

		final byte[] values = new byte[1000];
		for ( int i = 0; i < 1000; i++ ) {
			values[i] = (byte) i;
		}

		Assert.assertEquals( -6925839588689899575L, xxHasher.getHash( values ) );
	}
}
