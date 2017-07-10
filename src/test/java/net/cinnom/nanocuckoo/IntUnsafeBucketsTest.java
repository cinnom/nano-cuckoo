package net.cinnom.nanocuckoo;

import org.junit.Assert;
import org.junit.Test;

/**
 * IntUnsafeBuckets tests.
 */
public class IntUnsafeBucketsTest {

	@Test
	public void multiEntriesBucketsTest() {

		final int entries = 4;
		final long buckets = 32;
		final boolean counting = false;

		final IntUnsafeBuckets intUnsafeBuckets = new IntUnsafeBuckets( entries, buckets, counting );

		for(int e = 0; e < entries; e++) {
			for(int b = 0; b < buckets; b++) {

				int value = (23 + e * b);
				intUnsafeBuckets.putValue( e, b, value );
				Assert.assertEquals(value, intUnsafeBuckets.getValue( e, b ));
			}
		}

		intUnsafeBuckets.close();
	}
}
