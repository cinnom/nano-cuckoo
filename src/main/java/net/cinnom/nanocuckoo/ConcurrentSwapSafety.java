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
package net.cinnom.nanocuckoo;

/**
 * Used to set insert swap safety (and speed).
 */
public enum ConcurrentSwapSafety {

	/**
	 * Default insert safety. Only one thread can swap values at a time. Safe to use without losing values in highly
	 * concurrent situations.
	 */
	RELIABLE,
	/**
	 * Fast insert safety. Swaps are not locked. Can lose a number of previously inserted values up to (concurrent
	 * insertion threads - 1) when the filter hits max load. Also more likely to get a false-negative when doing
	 * concurrent inserts/contains, but should still be extremely rare.
	 */
	FAST,
	/**
	 * "Smart" insert safety. Will use FAST insert safety when below a given load factor (default 90%), and then
	 * switches to RELIABLE insert safety.
	 */
	SMART

}
