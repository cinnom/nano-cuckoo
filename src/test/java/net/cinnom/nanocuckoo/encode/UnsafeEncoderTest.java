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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

/**
 * UnsafeEncoder tests
 */
public class UnsafeEncoderTest {

	@Test
	public void encodeTest() {

		final UnsafeEncoder unsafeEncoder = new UnsafeEncoder();

		final String testStr = "ve09jw@$%THafw09\uD83D\uDD8F";

		byte[] values;
		if ( ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ) {
			values = testStr.getBytes( StandardCharsets.UTF_16LE );
		} else {
			values = testStr.getBytes( StandardCharsets.UTF_16BE );
		}

		int i = 0;
		for ( byte oneByte : unsafeEncoder.encode( testStr ) ) {
			Assert.assertEquals( values[i++], oneByte );
		}
	}

	@Test
	public void noSuchFieldExceptionTest() throws NoSuchFieldException {

		final UnsafeEncoder unsafeEncoder = new UnsafeEncoder();
		final UnsafeEncoder spyUnsafeEncoder = spy( unsafeEncoder );

		doThrow( NoSuchFieldException.class ).when( spyUnsafeEncoder ).getStringValueField();

		try {
			spyUnsafeEncoder.init();
			Assert.fail();
		} catch ( final RuntimeException ex ) {
			Assert.assertEquals( ex.getMessage(), "Failed to get field offsets" );
		}
	}
}
