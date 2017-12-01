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

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * Gives access to {@link Unsafe}.
 */
public class UnsafeProvider {

	private Unsafe unsafe;

	/**
	 * Instantiate an UnsafeProvider. Will try to get an {@link Unsafe}, and will throw a RuntimeException on failure.
	 */
	public UnsafeProvider() {

		init();
	}

	void init() {

		try {
			Field unsafeField = getUnsafeField();
			unsafeField.setAccessible( true );
			unsafe = (Unsafe) unsafeField.get( null );
		} catch ( final Throwable e ) {
			throw new RuntimeException( "Failed to obtain Unsafe", e );
		}
	}

	/**
	 * Get an instance of {@link Unsafe}.
	 */
	public Unsafe getUnsafe() {

		return unsafe;
	}

	Field getUnsafeField() throws NoSuchFieldException {

		return Unsafe.class.getDeclaredField( "theUnsafe" );
	}
}
