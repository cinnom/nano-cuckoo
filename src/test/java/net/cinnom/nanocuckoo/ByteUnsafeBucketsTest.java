package net.cinnom.nanocuckoo;

import org.junit.Assert;
import org.junit.Test;

/**
 * ByteUnsafeBuckets tests.
 */
public class ByteUnsafeBucketsTest {

	@Test
	public void multiEntriesBucketsTest() {

		final int entries = 4;
		final long buckets = 32;
		final boolean counting = false;

		final ByteUnsafeBuckets byteUnsafeBuckets = new ByteUnsafeBuckets( entries, buckets, counting );

		for(int e = 0; e < entries; e++) {
			for(int b = 0; b < buckets; b++) {

				int value = (23 + e * b);
				byteUnsafeBuckets.putValue( e, b, value );
				Assert.assertEquals(value, byteUnsafeBuckets.getValue( e, b ));
			}
		}

		byteUnsafeBuckets.close();
	}
}
