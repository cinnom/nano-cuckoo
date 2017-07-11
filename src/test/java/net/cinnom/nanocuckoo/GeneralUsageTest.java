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

import org.junit.Assert;
import org.junit.Test;

/**
 * General usage test. This is really an integration test, but it runs quickly enough to be a unit test.
 */
public class GeneralUsageTest {

	@Test
	public void generalUsageTest() {

		long capacity = 32;

		// Use Builder to create a NanoCuckooFilter. Only required parameter is capacity.
		final NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( capacity ).withCountingEnabled( true ) // Enable counting
				.build();

		Assert.assertEquals( capacity, cuckooFilter.getCapacity() );

		String testValue = "test value";

		// Insert a value into the filter
		cuckooFilter.insert( testValue );

		// Check that the value is in the filter
		boolean isValueInFilter = cuckooFilter.contains( testValue ); // Returns true

		Assert.assertTrue( isValueInFilter );

		// Check that some other value is in the filter
		isValueInFilter = cuckooFilter.contains( "some other value" ); // Should return false, probably

		// Generally wouldn't want to assert this, but it will be fine here
		Assert.assertFalse( isValueInFilter );

		// Insert the same value a couple more times
		cuckooFilter.insert( testValue );
		cuckooFilter.insert( testValue );

		// Get a count of how many times the value is in the filter
		int insertedCount = cuckooFilter.count( testValue ); // Returns 3 since we inserted three times with counting enabled

		Assert.assertEquals( 3, insertedCount );

		// Delete value from the filter once
		boolean wasDeleted = cuckooFilter.delete( testValue ); // Returns true since a value was deleted

		Assert.assertTrue( wasDeleted );

		// Try to delete the value up to six more times
		int deletedCount = cuckooFilter.delete( testValue, 6 ); // Returns 2 since only two copies of the value were left

		Assert.assertEquals( 2, deletedCount );

		isValueInFilter = cuckooFilter.contains( testValue ); // Returns false since all copies of the value were deleted

		Assert.assertFalse( isValueInFilter );

		// Double filter capacity by doubling entries per bucket. However, this also roughly doubles max FPP.
		cuckooFilter.expand();

		Assert.assertEquals( capacity * 2, cuckooFilter.getCapacity() );

		// Close the filter when finished with it. This is important!
		cuckooFilter.close();
	}

}
