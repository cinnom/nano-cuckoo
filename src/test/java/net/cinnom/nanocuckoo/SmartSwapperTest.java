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

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SmartSwapper tests.
 */
public class SmartSwapperTest {

	@Test
	public void fastSwap() {

		Swapper fastSwapper = mock( Swapper.class );
		Swapper reliableSwapper = mock( Swapper.class );
		UnsafeBuckets buckets = mock( UnsafeBuckets.class );
		final double smartInsertLoadFactor = 0.90;

		when( buckets.getCapacity() ).thenReturn( 100L );
		when( buckets.getInsertedCount() ).thenReturn( 89L );

		int fingerprint = 123;
		long bucket = 123456789L;

		SmartSwapper smartSwapper = new SmartSwapper( fastSwapper, reliableSwapper, buckets, smartInsertLoadFactor );

		smartSwapper.swap( fingerprint, bucket );

		verify( fastSwapper ).swap( fingerprint, bucket );
		verify( reliableSwapper, never() ).swap( fingerprint, bucket );
	}

	@Test
	public void reliableSwap() {

		Swapper fastSwapper = mock( Swapper.class );
		Swapper reliableSwapper = mock( Swapper.class );
		UnsafeBuckets buckets = mock( UnsafeBuckets.class );
		final double smartInsertLoadFactor = 0.90;

		when( buckets.getCapacity() ).thenReturn( 100L );
		when( buckets.getInsertedCount() ).thenReturn( 91L );

		int fingerprint = 123;
		long bucket = 123456789L;

		SmartSwapper smartSwapper = new SmartSwapper( fastSwapper, reliableSwapper, buckets, smartInsertLoadFactor );

		smartSwapper.swap( fingerprint, bucket );

		verify( reliableSwapper ).swap( fingerprint, bucket );
		verify( fastSwapper, never() ).swap( fingerprint, bucket );
	}
}
