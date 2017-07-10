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

import java.io.Serializable;

/**
 * Hashes fingerprints by multiplying them by a fixed value. Much better throughput compared to XXHasher for
 * fingerprints. Credit goes to CuckooFilter4J for this idea.
 */
public final class FixedHasher implements FingerprintHasher, Serializable {

	private static final long serialVersionUID = 1L;

	private static final long MURMUR_HASH_MIX = 0xC4CEB9FE1A85EC53L;

	/**
	 * "Hash" a fingerprint by multiplying it by 0xC4CEB9FE1A85EC53L.
	 * 
	 * @param value
	 *            Value to hash.
	 * @return 64-bit hash.
	 */
	@Override
	public long getHash( int value ) {

		return value * MURMUR_HASH_MIX;
	}
}
