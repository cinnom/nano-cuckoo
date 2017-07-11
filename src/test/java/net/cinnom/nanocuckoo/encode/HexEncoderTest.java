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
 * HexEncoder tests
 */
public class HexEncoderTest {

	@Test
	public void encodeTest() {

		final HexEncoder hexEncoder = new HexEncoder();

		final String testStr = "123456789abCDEf0";

		final byte[] values = new byte[] { 18, 52, 86, 120, (byte) 154, (byte) 188, (byte) 222, (byte) 240 };

		int i = 0;
		for ( byte oneByte : hexEncoder.encode( testStr ) ) {
			Assert.assertEquals( values[i++], oneByte );
		}
	}

}
