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
