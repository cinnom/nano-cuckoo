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

import org.junit.Assert;
import org.junit.Test;

/**
 * ASCIIEncoder tests
 */
public class ASCIIEncoderTest {

	@Test
	public void encodeTest() {

		final ASCIIEncoder asciiEncoder = new ASCIIEncoder();

		// All ASCII characters from 32 to 126
		final String testStr = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

		int i = 32;
		for(byte oneByte : asciiEncoder.encode( testStr ) ) {
			Assert.assertEquals(i++, oneByte);
		}
	}

}
