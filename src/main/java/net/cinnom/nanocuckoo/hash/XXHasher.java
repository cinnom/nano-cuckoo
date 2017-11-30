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

import net.jpountz.xxhash.XXHashFactory;

/**
 * XXHash 64-bit bucket hasher. Very fast (non-cryptographic) hash with a good distribution.
 */
public class XXHasher implements BucketHasher {

	private static final int DEFAULT_SEED = 0x48F7E28A;

	private final int seed;
	private XXHashFactory factory = XXHashFactory.unsafeInstance();

	/**
	 * Instantiate the XXHasher with the given random seed.
	 *
	 * @param seed Random seed.
	 */
	public XXHasher( int seed ) {

		this.seed = seed;
	}

	/**
	 * Instantiate the XXHasher with the default random seed {@value #DEFAULT_SEED}.
	 */
	public XXHasher() {

		this( DEFAULT_SEED );
	}

	/**
	 * Get 64-bit bucket hash using XXHash.
	 *
	 * @param data Data to hash.
	 * @return 64-bit hash.
	 */
	@Override
	public long getHash( byte[] data ) {

		return factory.hash64().hash( data, 0, data.length, seed );
	}
}
