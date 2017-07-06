package org.cinnom.nanocuckoo;

/**
 * Used to set insert safety (and speed).
 */
public enum InsertSafety {

	/**
	 * Normal insert speed. Only one thread can swap values at a time. Safe to use without losing values in highly
	 * concurrent situations.
	 */
	NORMAL,
	/**
	 * Fast insert speed. No extra locking during swaps. Can lose a number of previously inserted values equal one less
	 * than the number of concurrent threads that are inserting when the filter hits max load. Also more likely to get a
	 * false-negative when doing concurrent inserts/contains, but should still be extremely rare.
	 */
	FAST,
	/**
	 * "Smart" insert speed. Will use FAST insert speed when below a given load factor (default 90%), and then switches
	 * to NORMAL insert speed.
	 */
	SMART;

}
