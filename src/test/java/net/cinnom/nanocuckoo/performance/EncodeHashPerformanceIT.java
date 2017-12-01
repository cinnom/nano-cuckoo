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
package net.cinnom.nanocuckoo.performance;

import org.junit.Test;

import net.cinnom.nanocuckoo.NanoCuckooFilter;
import net.cinnom.nanocuckoo.encode.ASCIIEncoder;
import net.cinnom.nanocuckoo.encode.HexEncoder;
import net.cinnom.nanocuckoo.encode.StringEncoder;
import net.cinnom.nanocuckoo.encode.UTF16LEEncoder;
import net.cinnom.nanocuckoo.encode.UTF8Encoder;
import net.cinnom.nanocuckoo.encode.UnsafeEncoder;
import net.cinnom.nanocuckoo.hash.BucketHasher;
import net.cinnom.nanocuckoo.hash.MetroHasher;
import net.cinnom.nanocuckoo.hash.XXHasher;

/**
 * Informal encoder+hasher performance tests
 */
public class EncodeHashPerformanceIT {

	private final BucketHasher xxHasher = new XXHasher( NanoCuckooFilter.Builder.DEFAULT_SEED );
	private final BucketHasher metroHasher = new MetroHasher( NanoCuckooFilter.Builder.DEFAULT_SEED );

	private final StringEncoder asciiEncoder = new ASCIIEncoder();
	private final StringEncoder hexEncoder = new HexEncoder();
	private final StringEncoder utf8Encoder = new UTF8Encoder();
	private final StringEncoder utf16LEEncoder = new UTF16LEEncoder();
	private final StringEncoder unsafeEncoder = new UnsafeEncoder();

	private final String smallStr = "123456789abCDEf";
	private final String midStr = "123456789abCDEf0123456789ABcdeF123456";
	private final String bigStr = "123456789abCDEf0123456789ABcdeF123456789abCDEf0123456789ABcdeF123456789abCDEf0123456"
			+ "789ABcdeF123456789abCDEf0123456789ABcdeF123456789abCDEf0123456789ABcdeF123456789abCDEf0123456789ABcdeF";
	private final int iterations = 250_000_000;

	public EncodeHashPerformanceIT() throws NoSuchFieldException {

	}

	@Test
	public void asciiBigEncodeMetroTest() {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( asciiEncoder.encode( bigStr ) );
		}
	}

	@Test
	public void hexBigEncodeMetroTest() {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( hexEncoder.encode( bigStr ) );
		}
	}

	@Test
	public void utf8BigEncodeMetroTest() {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( utf8Encoder.encode( bigStr ) );
		}
	}

	@Test
	public void utf16LEBigEncodeMetroTest() {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( utf16LEEncoder.encode( bigStr ) );
		}
	}

	@Test
	public void unsafeBigEncodeMetroTest() throws NoSuchFieldException {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( unsafeEncoder.encode( bigStr ) );
		}
	}

	@Test
	public void asciiMidEncodeMetroTest() {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( asciiEncoder.encode( midStr ) );
		}
	}

	@Test
	public void hexMidEncodeMetroTest() {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( hexEncoder.encode( midStr ) );
		}
	}

	@Test
	public void utf8MidEncodeMetroTest() {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( utf8Encoder.encode( midStr ) );
		}
	}

	@Test
	public void utf16LEMidEncodeMetroTest() {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( utf16LEEncoder.encode( midStr ) );
		}
	}

	@Test
	public void unsafeMidEncodeMetroTest() throws NoSuchFieldException {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( unsafeEncoder.encode( midStr ) );
		}
	}

	@Test
	public void asciiSmallEncodeMetroTest() {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( asciiEncoder.encode( smallStr ) );
		}
	}

	@Test
	public void hexSmallEncodeMetroTest() {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( hexEncoder.encode( smallStr ) );
		}
	}

	@Test
	public void utf8SmallEncodeMetroTest() {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( utf8Encoder.encode( smallStr ) );
		}
	}

	@Test
	public void utf16LESmallEncodeMetroTest() {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( utf16LEEncoder.encode( smallStr ) );
		}
	}

	@Test
	public void unsafeSmallEncodeMetroTest() throws NoSuchFieldException {

		for ( int i = 0; i < iterations; i++ ) {
			metroHasher.getHash( unsafeEncoder.encode( smallStr ) );
		}
	}

	@Test
	public void asciiBigEncodeXXTest() {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( asciiEncoder.encode( bigStr ) );
		}
	}

	@Test
	public void hexBigEncodeXXTest() {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( hexEncoder.encode( bigStr ) );
		}
	}

	@Test
	public void utf8BigEncodeXXTest() {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( utf8Encoder.encode( bigStr ) );
		}
	}

	@Test
	public void utf16LEBigEncodeXXTest() {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( utf16LEEncoder.encode( bigStr ) );
		}
	}

	@Test
	public void unsafeBigEncodeXXTest() throws NoSuchFieldException {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( unsafeEncoder.encode( bigStr ) );
		}
	}

	@Test
	public void asciiMidEncodeXXTest() {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( asciiEncoder.encode( midStr ) );
		}
	}

	@Test
	public void hexMidEncodeXXTest() {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( hexEncoder.encode( midStr ) );
		}
	}

	@Test
	public void utf8MidEncodeXXTest() {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( utf8Encoder.encode( midStr ) );
		}
	}

	@Test
	public void utf16LEMidEncodeXXTest() {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( utf16LEEncoder.encode( midStr ) );
		}
	}

	@Test
	public void unsafeMidEncodeXXTest() throws NoSuchFieldException {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( unsafeEncoder.encode( midStr ) );
		}
	}

	@Test
	public void asciiSmallEncodeXXTest() {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( asciiEncoder.encode( smallStr ) );
		}
	}

	@Test
	public void hexSmallEncodeXXTest() {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( hexEncoder.encode( smallStr ) );
		}
	}

	@Test
	public void utf8SmallEncodeXXTest() {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( utf8Encoder.encode( smallStr ) );
		}
	}

	@Test
	public void utf16LESmallEncodeXXTest() {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( utf16LEEncoder.encode( smallStr ) );
		}
	}

	@Test
	public void unsafeSmallEncodeXXTest() throws NoSuchFieldException {

		for ( int i = 0; i < iterations; i++ ) {
			xxHasher.getHash( unsafeEncoder.encode( smallStr ) );
		}
	}
}
