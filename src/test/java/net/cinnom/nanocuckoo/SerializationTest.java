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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.cinnom.nanocuckoo.encode.UnsafeEncoder;
import org.junit.Assert;
import org.junit.Test;

import net.cinnom.nanocuckoo.dumb.DumbBucketHasher;
import net.cinnom.nanocuckoo.dumb.DumbFingerprintHasher;
import net.cinnom.nanocuckoo.dumb.DumbRandomInt;
import net.cinnom.nanocuckoo.dumb.DumbStringEncoder;
import net.cinnom.nanocuckoo.encode.ASCIIEncoder;
import net.cinnom.nanocuckoo.encode.HexEncoder;
import net.cinnom.nanocuckoo.encode.UTF16LEEncoder;
import net.cinnom.nanocuckoo.encode.UTF8Encoder;
import net.cinnom.nanocuckoo.hash.FixedHasher;
import net.cinnom.nanocuckoo.hash.MetroHasher;
import net.cinnom.nanocuckoo.hash.XXHasher;
import net.cinnom.nanocuckoo.random.WrappedThreadLocalRandom;

/**
 * Serialization test. This is really an integration test, but it runs quickly enough to be a unit test.
 */
public class SerializationTest {

	private static final byte CUSTOM_TYPE = 0;

	private static final byte UTF8_ENCODER_TYPE = 1;
	private static final byte UTF16LE_ENCODER_TYPE = 2;
	private static final byte ASCII_ENCODER_TYPE = 3;
	private static final byte HEX_ENCODER_TYPE = 4;
	private static final byte UNSAFE_ENCODER_TYPE = 5;

	private static final byte XXHASHER_BUCKET_HASHER_TYPE = 1;
	private static final byte METROHASHER_BUCKET_HASHER_TYPE = 2;

	private static final byte FIXED_FP_HASHER_TYPE = 1;

	private static final byte THREADLOCAL_RANDOM_INT_TYPE = 1;

	private final Serialization serialization = new Serialization();

	@Test
	public void serializationTest() throws IOException, ClassNotFoundException {

		// Create a filter
		NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( 32 ).build();

		String testValue = "test value";

		// Insert a value into the filter
		cuckooFilter.insert( testValue );

		// Serialize the filter to a byte array
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream( byteOutputStream );
		objectOutputStream.writeObject( cuckooFilter );
		objectOutputStream.close();
		byte[] serializedBytes = byteOutputStream.toByteArray();

		System.out.println( "Serialized filter size: " + serializedBytes.length );

		// Close the current filter before replacing it
		cuckooFilter.close();

		// Read the serialized filter in from the byte array
		ByteArrayInputStream byteInputStream = new ByteArrayInputStream( serializedBytes );
		ObjectInputStream objectInputStream = new ObjectInputStream( byteInputStream );
		cuckooFilter = (NanoCuckooFilter) objectInputStream.readObject();
		objectInputStream.close();

		boolean isValueInFilter = cuckooFilter.contains( testValue ); // Returns true

		Assert.assertTrue( isValueInFilter );

		// Close the filter
		cuckooFilter.close();
	}

	@Test
	public void dumpMemoryTest() throws IOException {

		// Create a filter
		NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( 32 ).build();

		String testValue = "test value";

		// Insert a value into the filter
		cuckooFilter.insert( testValue );

		// Dump filter internal memory
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		cuckooFilter.writeMemory( byteOutputStream );
		byteOutputStream.close();
		byte[] serializedBytes = byteOutputStream.toByteArray();

		System.out.println( "Filter memory size: " + serializedBytes.length );

		// Close the current filter before replacing it
		cuckooFilter.close();

		// New Filter needs to be built with the same parameters as the previous filter
		cuckooFilter = new NanoCuckooFilter.Builder( 32 ).build();
		// Read the dumped memory in from the byte array
		ByteArrayInputStream byteInputStream = new ByteArrayInputStream( serializedBytes );
		cuckooFilter.readMemory( byteInputStream );
		byteInputStream.close();

		boolean isValueInFilter = cuckooFilter.contains( testValue ); // Returns true

		Assert.assertTrue( isValueInFilter );

		// Close the filter
		cuckooFilter.close();
	}

	@Test
	public void serializeCustomTypesTest() throws IOException, ClassNotFoundException {

		NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( 32 ).withStringEncoder( new DumbStringEncoder() )
				.withBucketHasher( new DumbBucketHasher() ).withFingerprintHasher( new DumbFingerprintHasher() )
				.withRandomInt( new DumbRandomInt() ).build();

		String testValue = "test value";

		// Insert a value into the filter
		cuckooFilter.insert( testValue );

		// Serialize the filter to a byte array
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream( byteOutputStream );
		objectOutputStream.writeObject( cuckooFilter );
		byte[] serializedBytes = byteOutputStream.toByteArray();
		objectOutputStream.close();

		System.out.println( "Custom serialized filter size: " + serializedBytes.length );

		// Close the current filter before replacing it
		cuckooFilter.close();

		// Read the serialized filter in from the byte array
		ByteArrayInputStream byteInputStream = new ByteArrayInputStream( serializedBytes );
		ObjectInputStream objectInputStream = new ObjectInputStream( byteInputStream );
		cuckooFilter = (NanoCuckooFilter) objectInputStream.readObject();
		objectInputStream.close();

		boolean isValueInFilter = cuckooFilter.contains( testValue ); // Returns true

		Assert.assertTrue( isValueInFilter );

		// Close the filter
		cuckooFilter.close();
	}

	@Test
	public void saveLoadTest() throws IOException, ClassNotFoundException {

		// Create a filter
		NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( 32 ).build();

		String testValue = "test value";

		// Insert a value into the filter
		cuckooFilter.insert( testValue );

		// Serialize the filter to a byte array
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream( byteOutputStream );
		cuckooFilter.saveFilter( objectOutputStream );
		objectOutputStream.close();
		byte[] serializedBytes = byteOutputStream.toByteArray();

		System.out.println( "Saved filter size: " + serializedBytes.length );

		// Close the current filter before replacing it
		cuckooFilter.close();

		// Read the serialized filter in from the byte array
		ByteArrayInputStream byteInputStream = new ByteArrayInputStream( serializedBytes );
		ObjectInputStream objectInputStream = new ObjectInputStream( byteInputStream );
		cuckooFilter = NanoCuckooFilter.loadFilter( objectInputStream );
		objectInputStream.close();

		boolean isValueInFilter = cuckooFilter.contains( testValue ); // Returns true

		Assert.assertTrue( isValueInFilter );

		// Close the filter
		cuckooFilter.close();
	}

	@Test
	public void customStringEncoderTypeTest() {

		Assert.assertEquals( CUSTOM_TYPE, serialization.getStringEncoderType( new DumbStringEncoder() ) );
		Assert.assertTrue( serialization.createStringEncoder( CUSTOM_TYPE ) instanceof UTF16LEEncoder );
	}

	@Test
	public void asciiStringEncoderTypeTest() {

		Assert.assertEquals( ASCII_ENCODER_TYPE, serialization.getStringEncoderType( new ASCIIEncoder() ) );
		Assert.assertTrue( serialization.createStringEncoder( ASCII_ENCODER_TYPE ) instanceof ASCIIEncoder );
	}

	@Test
	public void hexStringEncoderTypeTest() {

		Assert.assertEquals( HEX_ENCODER_TYPE, serialization.getStringEncoderType( new HexEncoder() ) );
		Assert.assertTrue( serialization.createStringEncoder( HEX_ENCODER_TYPE ) instanceof HexEncoder );
	}

	@Test
	public void utf8StringEncoderTypeTest() {

		Assert.assertEquals( UTF8_ENCODER_TYPE, serialization.getStringEncoderType( new UTF8Encoder() ) );
		Assert.assertTrue( serialization.createStringEncoder( UTF8_ENCODER_TYPE ) instanceof UTF8Encoder );
	}

	@Test
	public void unsafeStringEncoderTypeTest() {

		Assert.assertEquals( UNSAFE_ENCODER_TYPE, serialization.getStringEncoderType( new UnsafeEncoder() ) );
		Assert.assertTrue( serialization.createStringEncoder( UNSAFE_ENCODER_TYPE ) instanceof UnsafeEncoder );
	}

	@Test
	public void utf16StringEncoderTypeTest() {

		Assert.assertEquals( UTF16LE_ENCODER_TYPE, serialization.getStringEncoderType( new UTF16LEEncoder() ) );
		Assert.assertTrue( serialization.createStringEncoder( UTF16LE_ENCODER_TYPE ) instanceof UTF16LEEncoder );
	}

	@Test
	public void customBucketHasherTypeTest() {

		Assert.assertEquals( CUSTOM_TYPE, serialization.getBucketHasherType( new DumbBucketHasher() ) );
		Assert.assertTrue( serialization.createBucketHasher( CUSTOM_TYPE, 1 ) instanceof XXHasher );
	}

	@Test
	public void xxHasherBucketHasherTypeTest() {

		Assert.assertEquals( XXHASHER_BUCKET_HASHER_TYPE, serialization.getBucketHasherType( new XXHasher( 1 ) ) );
		Assert.assertTrue( serialization.createBucketHasher( XXHASHER_BUCKET_HASHER_TYPE, 1 ) instanceof XXHasher );
	}

	@Test
	public void metroHasherBucketHasherTypeTest() {

		Assert.assertEquals( METROHASHER_BUCKET_HASHER_TYPE,
				serialization.getBucketHasherType( new MetroHasher( 1 ) ) );
		Assert.assertTrue(
				serialization.createBucketHasher( METROHASHER_BUCKET_HASHER_TYPE, 1 ) instanceof MetroHasher );
	}

	@Test
	public void customFingerprintHasherTypeTest() {

		Assert.assertEquals( CUSTOM_TYPE, serialization.getFingerprintHasherType( new DumbFingerprintHasher() ) );
		Assert.assertTrue( serialization.createFingerprintHasher( CUSTOM_TYPE ) instanceof FixedHasher );
	}

	@Test
	public void fixedFingerprintHasherTypeTest() {

		Assert.assertEquals( FIXED_FP_HASHER_TYPE, serialization.getFingerprintHasherType( new FixedHasher() ) );
		Assert.assertTrue( serialization.createFingerprintHasher( FIXED_FP_HASHER_TYPE ) instanceof FixedHasher );
	}

	@Test
	public void customRandomIntTypeTest() {

		Assert.assertEquals( CUSTOM_TYPE, serialization.getRandomIntType( new DumbRandomInt() ) );
		Assert.assertTrue( serialization.createRandomInt( CUSTOM_TYPE ) instanceof WrappedThreadLocalRandom );
	}

	@Test
	public void threadLocalRandomIntTypeTest() {

		Assert.assertEquals( THREADLOCAL_RANDOM_INT_TYPE,
				serialization.getRandomIntType( new WrappedThreadLocalRandom() ) );
		Assert.assertTrue(
				serialization.createRandomInt( THREADLOCAL_RANDOM_INT_TYPE ) instanceof WrappedThreadLocalRandom );
	}
}
