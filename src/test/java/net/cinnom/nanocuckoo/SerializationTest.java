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

import org.junit.Assert;
import org.junit.Test;

/**
 * Serialization test. This is really an integration test, but it runs quickly enough to be a unit test.
 */
public class SerializationTest {

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
		byte[] serializedBytes = byteOutputStream.toByteArray();

		System.out.println( "Serialized filter size: " + serializedBytes.length );

		// Close the current filter before replacing it
		cuckooFilter.close();

		// Read the serialized filter in from the byte array
		ByteArrayInputStream byteInputStream = new ByteArrayInputStream( serializedBytes );
		ObjectInputStream objectInputStream = new ObjectInputStream( byteInputStream );
		cuckooFilter = (NanoCuckooFilter) objectInputStream.readObject();

		boolean isValueInFilter = cuckooFilter.contains( testValue ); // Returns true

		Assert.assertTrue( isValueInFilter );

		// Close the filter
		cuckooFilter.close();
	}

	@Test
	public void dumpMemoryTest() throws IOException, ClassNotFoundException {

		// Create a filter
		NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( 32 ).build();

		String testValue = "test value";

		// Insert a value into the filter
		cuckooFilter.insert( testValue );

		// Dump filter internal memory
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		cuckooFilter.writeMemory( byteOutputStream );
		byte[] serializedBytes = byteOutputStream.toByteArray();

		System.out.println( "Filter memory size: " + serializedBytes.length );

		// Close the current filter before replacing it
		cuckooFilter.close();

		// New Filter needs to be built with the same parameters as the previous filter
		cuckooFilter = new NanoCuckooFilter.Builder( 32 ).build();
		// Read the dumped memory in from the byte array
		ByteArrayInputStream byteInputStream = new ByteArrayInputStream( serializedBytes );
		cuckooFilter.readMemory( byteInputStream );

		boolean isValueInFilter = cuckooFilter.contains( testValue ); // Returns true

		Assert.assertTrue( isValueInFilter );

		// Close the filter
		cuckooFilter.close();
	}
}
