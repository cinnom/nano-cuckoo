package net.cinnom.nanocuckoo;

import org.junit.Assert;
import org.junit.Test;

/**
 * VariableUnsafeBuckets tests.
 */
public class VariableUnsafeBucketsTest {

	@Test
	public void multiEntriesBucketsTest() {

		final int entries = 4;
		final long buckets = 32;
		final boolean counting = false;

		for(int fp = 1; fp <= 32; fp++) {

			final VariableUnsafeBuckets varUnsafeBuckets = new VariableUnsafeBuckets( entries, buckets, fp, counting );

			for ( int e = 0; e < entries; e++ ) {
				for ( int b = 0; b < buckets; b++ ) {

					int fpMask = -1 >>> (32 - fp);
					int value = ( 1023 + e * b ) & fpMask;
					varUnsafeBuckets.putValue( e, b, value );
					Assert.assertEquals( value, varUnsafeBuckets.getValue( e, b ) );
				}
			}
		}
	}
}
