package org.cinnom.nanocuckoo;

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
