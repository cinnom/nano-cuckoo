package org.cinnom.nanocuckoo;

import org.junit.Test;

/**
 * Created by rjones on 6/28/17.
 */
public class VariableUnsafeBucketsTest {

	@Test
	public void test() {

		for ( short b = 0; b < 128; b++ ) {
			char c = (char) b;
			System.out.println( b + ": " + c );
		}
	}

}
