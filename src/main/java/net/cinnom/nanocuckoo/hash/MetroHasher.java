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

import net.cinnom.nanocuckoo.metro.UnsafeMetroHash64;

/**
 * MetroHash 64-bit bucket hasher. Very fast (non-cryptographic) hash with a good distribution.
 */
public class MetroHasher implements BucketHasher {

	private final int seed;
	private final UnsafeMetroHash64 metroHash64 = new UnsafeMetroHash64();

	/**
	 * Instantiate the MetroHasher with the given random seed.
	 *
	 * @param seed
	 *            Random seed.
	 */
	public MetroHasher( int seed ) {

		this.seed = seed;
	}

	/**
	 * Get 64-bit bucket hash using MetroHash.
	 *
	 * @param data
	 *            Data to hash.
	 * @return 64-bit hash.
	 */
	@Override
	public long getHash( byte[] data ) {

		return metroHash64.hash( data, 0, data.length, seed );
	}

	@Override
	public int getSeed() {

		return seed;
	}
}
