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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;

public class UnsafeProviderTest {

	@Test
	public void notNullTest() {

		Assert.assertNotNull( new UnsafeProvider().getUnsafe() );
	}

	@Test
	public void noSuchFieldExceptionTest() throws NoSuchFieldException {

		final UnsafeProvider unsafeProvider = new UnsafeProvider();

		final UnsafeProvider spyUnsafeProvider = spy( unsafeProvider );

		doThrow( NoSuchFieldException.class ).when( spyUnsafeProvider ).getUnsafeField();

		try {
			spyUnsafeProvider.init();
			Assert.fail();
		} catch ( final RuntimeException ex ) {
			Assert.assertEquals( ex.getMessage(), "Failed to obtain Unsafe" );
		}
	}

	@Test
	public void illegalAccessExceptionTest() throws NoSuchFieldException, IllegalAccessException {

		final UnsafeProvider unsafeProvider = new UnsafeProvider();

		final UnsafeProvider spyUnsafeProvider = spy( unsafeProvider );
		final Field unsafeField = mock( Field.class );

		doReturn( unsafeField ).when( spyUnsafeProvider ).getUnsafeField();
		when( unsafeField.get( null ) ).thenThrow( IllegalAccessException.class );

		try {
			spyUnsafeProvider.init();
			Assert.fail();
		} catch ( final RuntimeException ex ) {
			Assert.assertEquals( ex.getMessage(), "Failed to obtain Unsafe" );
		}
	}
}
