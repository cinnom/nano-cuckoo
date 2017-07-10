package net.cinnom.nanocuckoo;

import org.junit.Assert;
import org.junit.Test;

/**
 * ShortUnsafeBuckets tests.
 */
public class ShortUnsafeBucketsTest {

	@Test
	public void multiEntriesBucketsTest() {

		final int entries = 4;
		final long buckets = 32;
		final boolean counting = false;

		final ShortUnsafeBuckets shortUnsafeBuckets = new ShortUnsafeBuckets( entries, buckets, counting );

		for(int e = 0; e < entries; e++) {
			for(int b = 0; b < buckets; b++) {

				int value = (23 + e * b);
				shortUnsafeBuckets.putValue( e, b, value );
				Assert.assertEquals(value, shortUnsafeBuckets.getValue( e, b ));
			}
		}

		shortUnsafeBuckets.close();
	}
}
