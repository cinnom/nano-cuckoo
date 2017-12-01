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
package net.cinnom.nanocuckoo.hash;

import org.junit.Assert;
import org.junit.Test;

/**
 * FixedHasher tests.
 */
public class FixedHasherTest {

	@Test
	public void getHashTest() {

		final FixedHasher fixedHasher = new FixedHasher();

		final int testValue = 127;

		Assert.assertEquals( testValue * 0xC4CEB9FE1A85EC53L, fixedHasher.getHash( testValue ) );
	}
}
