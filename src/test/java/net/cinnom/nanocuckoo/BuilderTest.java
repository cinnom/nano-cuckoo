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

import net.cinnom.nanocuckoo.encode.UTF16LEEncoder;
import net.cinnom.nanocuckoo.hash.FixedHasher;
import net.cinnom.nanocuckoo.hash.XXHasher;
import org.junit.Assert;
import org.junit.Test;

/**
 * NanoCuckooFilter.Builder tests.
 */
public class BuilderTest {

	@Test
	public void invalidCapacityTest() {

		int capacity = 0;

		try {
			NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );
			Assert.assertTrue( false );
		} catch ( IllegalArgumentException ex ) {
		}
	}

	@Test
	public void validCapacityTest() {

		int capacity = 1;

		try {
			NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );
		} catch ( IllegalArgumentException ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void invalidEntriesPerBucketTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withEntriesPerBucket( 7 );
			Assert.assertTrue( false );
		} catch ( IllegalArgumentException ex ) {
		}
	}

	@Test
	public void validEntriesPerBucketTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withEntriesPerBucket( 8 );
		} catch ( IllegalArgumentException ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void invalidFingerprintBitsLowTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withFingerprintBits( 0 );
			Assert.assertTrue( false );
		} catch ( IllegalArgumentException ex ) {
		}
	}

	@Test
	public void invalidFingerprintBitsHighTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withFingerprintBits( 33 );
			Assert.assertTrue( false );
		} catch ( IllegalArgumentException ex ) {
		}
	}

	@Test
	public void validFingerprintBitsVariableTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withFingerprintBits( 30 );
		} catch ( IllegalArgumentException ex ) {
			Assert.assertTrue( false );
		}

		builder.build();
	}

	@Test
	public void validFingerprintBitsByteTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withFingerprintBits( 8 );
		} catch ( IllegalArgumentException ex ) {
			Assert.assertTrue( false );
		}

		builder.build();
	}

	@Test
	public void validFingerprintBitsShortTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withFingerprintBits( 16 );
		} catch ( IllegalArgumentException ex ) {
			Assert.assertTrue( false );
		}

		builder.build();
	}

	@Test
	public void validFingerprintBitsIntTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withFingerprintBits( 32 );
		} catch ( IllegalArgumentException ex ) {
			Assert.assertTrue( false );
		}

		builder.build();
	}

	@Test
	public void invalidMaxKicksTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withMaxKicks( -1 );
			Assert.assertTrue( false );
		} catch ( IllegalArgumentException ex ) {
		}
	}

	@Test
	public void validMaxKicksTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withMaxKicks( 33 );
		} catch ( IllegalArgumentException ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void invalidStringEncoderTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withStringEncoder( null );
			Assert.assertTrue( false );
		} catch ( IllegalArgumentException ex ) {
		}
	}

	@Test
	public void validStringEncoderTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withStringEncoder( new UTF16LEEncoder() );
		} catch ( IllegalArgumentException ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void invalidBucketHasherTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withBucketHasher( null );
			Assert.assertTrue( false );
		} catch ( IllegalArgumentException ex ) {
		}
	}

	@Test
	public void validBucketHasherTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withBucketHasher( new XXHasher( 123 ) );
		} catch ( IllegalArgumentException ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void invalidFingerprintHasherTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withFingerprintHasher( null );
			Assert.assertTrue( false );
		} catch ( IllegalArgumentException ex ) {
		}
	}

	@Test
	public void validFingerprintHasherTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withFingerprintHasher( new FixedHasher() );
		} catch ( IllegalArgumentException ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void invalidConcurrencyTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withConcurrency( 7 );
			Assert.assertTrue( false );
		} catch ( IllegalArgumentException ex ) {
		}
	}

	@Test
	public void validConcurrencyTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withConcurrency( 8 );
		} catch ( IllegalArgumentException ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void validCountingEnabledTest() {

		int capacity = 32;
		NanoCuckooFilter.Builder builder = new NanoCuckooFilter.Builder( capacity );

		try {
			builder.withCountingEnabled( true );
			builder.withCountingEnabled( false );
		} catch ( IllegalArgumentException ex ) {
			Assert.assertTrue( false );
		}
	}
}
