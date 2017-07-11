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

import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

/**
 * UTF16LEEncoder tests
 */
public class UTF16LEEncoderTest {

	@Test
	public void encodeTest() {

		final UTF16LEEncoder utf16leEncoder = new UTF16LEEncoder();

		final String testStr = "ve09jw@$%THafw09\uD83D\uDD8F";

		final byte[] values = testStr.getBytes( StandardCharsets.UTF_16LE );

		int i = 0;
		for ( byte oneByte : utf16leEncoder.encode( testStr ) ) {
			Assert.assertEquals( values[i++], oneByte );
		}
	}

}
