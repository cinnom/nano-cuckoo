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
package net.cinnom.nanocuckoo.encode;

import java.lang.reflect.Field;

import net.cinnom.nanocuckoo.UnsafeProvider;
import sun.misc.Unsafe;

/**
 * StringEncoder that will directly copy char[] to byte[] using {@link Unsafe}. Note that endianness of the resulting
 * byte[] will be system-dependent; this encoder should not be used across platforms that store characters in different
 * formats.
 * <p>
 * Performance on this encoder is better than all other encoders for Strings around 40 characters or more.
 * </p>
 */
public class UnsafeEncoder implements StringEncoder {

	private Unsafe unsafe;
	private long stringValueFieldOffset;

	/**
	 * Instantiate an UnsafeEncoder. Will try to get an {@link Unsafe} as well as field offsets, and will throw a
	 * RuntimeException on failure.
	 */
	public UnsafeEncoder() {

		init();
	}

	void init() {

		try {
			unsafe = new UnsafeProvider().getUnsafe();
			stringValueFieldOffset = unsafe.objectFieldOffset( getStringValueField() );
		} catch ( NoSuchFieldException ex ) {
			throw new RuntimeException( "Failed to get field offsets", ex );
		}
	}

	Field getStringValueField() throws NoSuchFieldException {

		return String.class.getDeclaredField( "value" );
	}

	/**
	 * Encode a String into UTF-16 with system-dependent endianness.
	 *
	 * @param data
	 *            String to encode.
	 * @return UTF-16 LE or BE bytes.
	 */
	@Override
	public byte[] encode( final String data ) {

		return (byte[]) unsafe.getObject( data, stringValueFieldOffset );
	}
}
