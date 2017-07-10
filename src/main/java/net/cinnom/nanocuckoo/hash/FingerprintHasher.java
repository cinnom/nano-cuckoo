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

/**
 * 64-bit Hasher for hashing fingerprint values.
 */
public interface FingerprintHasher {

	/**
	 * Hash a fingerprint value into 64 bits.
	 * 
	 * @param value
	 *            Value to hash.
	 * @return 64-bit hash.
	 */
	long getHash( int value );

}
