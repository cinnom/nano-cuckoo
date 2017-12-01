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

import net.cinnom.nanocuckoo.encode.ASCIIEncoder;
import net.cinnom.nanocuckoo.encode.HexEncoder;
import net.cinnom.nanocuckoo.encode.UTF16LEEncoder;
import net.cinnom.nanocuckoo.encode.UTF8Encoder;
import net.cinnom.nanocuckoo.encode.UnsafeEncoder;
import org.junit.Test;

/**
 * Informal encoder performance tests
 */
public class EncoderPerformanceIT {

	private final String smallStr = "123456789abCDEf";
	private final String midStr = "123456789abCDEf0123456789ABcdeF123456";
	private final String bigStr = "123456789abCDEf0123456789ABcdeF123456789abCDEf0123456789ABcdeF123456789abCDEf0123456"
			+ "789ABcdeF123456789abCDEf0123456789ABcdeF123456789abCDEf0123456789ABcdeF123456789abCDEf0123456789ABcdeF";
	private final int iterations = 500_000_000;

	@Test
	public void asciiBigEncodeTest() {

		final ASCIIEncoder asciiEncoder = new ASCIIEncoder();

		for ( int i = 0; i < iterations; i++ ) {
			asciiEncoder.encode( bigStr );
		}
	}

	@Test
	public void hexBigEncodeTest() {

		final HexEncoder hexEncoder = new HexEncoder();

		for ( int i = 0; i < iterations; i++ ) {
			hexEncoder.encode( bigStr );
		}
	}

	@Test
	public void utf8BigEncodeTest() {

		final UTF8Encoder utf8Encoder = new UTF8Encoder();

		for ( int i = 0; i < iterations; i++ ) {
			utf8Encoder.encode( bigStr );
		}
	}

	@Test
	public void utf16LEBigEncodeTest() {

		final UTF16LEEncoder utf16LEEncoder = new UTF16LEEncoder();

		for ( int i = 0; i < iterations; i++ ) {
			utf16LEEncoder.encode( bigStr );
		}
	}

	@Test
	public void unsafeBigEncodeTest() throws NoSuchFieldException {

		final UnsafeEncoder unsafeEncoder = new UnsafeEncoder();

		for ( int i = 0; i < iterations; i++ ) {
			unsafeEncoder.encode( bigStr );
		}
	}

	@Test
	public void asciiMidEncodeTest() {

		final ASCIIEncoder asciiEncoder = new ASCIIEncoder();

		for ( int i = 0; i < iterations; i++ ) {
			asciiEncoder.encode( midStr );
		}
	}

	@Test
	public void hexMidEncodeTest() {

		final HexEncoder hexEncoder = new HexEncoder();

		for ( int i = 0; i < iterations; i++ ) {
			hexEncoder.encode( midStr );
		}
	}

	@Test
	public void utf8MidEncodeTest() {

		final UTF8Encoder utf8Encoder = new UTF8Encoder();

		for ( int i = 0; i < iterations; i++ ) {
			utf8Encoder.encode( midStr );
		}
	}

	@Test
	public void utf16LEMidEncodeTest() {

		final UTF16LEEncoder utf16LEEncoder = new UTF16LEEncoder();

		for ( int i = 0; i < iterations; i++ ) {
			utf16LEEncoder.encode( midStr );
		}
	}

	@Test
	public void unsafeMidEncodeTest() throws NoSuchFieldException {

		final UnsafeEncoder unsafeEncoder = new UnsafeEncoder();

		for ( int i = 0; i < iterations; i++ ) {
			unsafeEncoder.encode( midStr );
		}
	}

	@Test
	public void asciiSmallEncodeTest() {

		final ASCIIEncoder asciiEncoder = new ASCIIEncoder();

		for ( int i = 0; i < iterations; i++ ) {
			asciiEncoder.encode( smallStr );
		}
	}

	@Test
	public void hexSmallEncodeTest() {

		final HexEncoder hexEncoder = new HexEncoder();

		for ( int i = 0; i < iterations; i++ ) {
			hexEncoder.encode( smallStr );
		}
	}

	@Test
	public void utf8SmallEncodeTest() {

		final UTF8Encoder utf8Encoder = new UTF8Encoder();

		for ( int i = 0; i < iterations; i++ ) {
			utf8Encoder.encode( smallStr );
		}
	}

	@Test
	public void utf16LESmallEncodeTest() {

		final UTF16LEEncoder utf16LEEncoder = new UTF16LEEncoder();

		for ( int i = 0; i < iterations; i++ ) {
			utf16LEEncoder.encode( smallStr );
		}
	}

	@Test
	public void unsafeSmallEncodeTest() throws NoSuchFieldException {

		final UnsafeEncoder unsafeEncoder = new UnsafeEncoder();

		for ( int i = 0; i < iterations; i++ ) {
			unsafeEncoder.encode( smallStr );
		}
	}
}
