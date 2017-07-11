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
package net.cinnom.nanocuckoo.concurrency;

import java.io.IOException;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import net.cinnom.nanocuckoo.ConcurrentSwapSafety;
import net.cinnom.nanocuckoo.NanoCuckooFilter;

/**
 * Insert/Contains/Delete concurrency test.
 */
public class InsertContainsDeleteIT {

	@Test
	public void insertContainsDeleteTest() throws InterruptedException, IOException, ClassNotFoundException {

		final int threads = 6;
		final int capacity = 10_000_000;
		final String containedString = "abcdefghijklmn-opqrstuvwxyz-000000";

		final NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( capacity ).withCountingEnabled( true )
				.withConcurrentSwapSafety( ConcurrentSwapSafety.SMART ).withFingerprintBits( 7 ).build();

		final AtomicInteger currentRun = new AtomicInteger();

		final Phaser startRunning = new Phaser( 1 + threads );
		final Phaser stopRunning = new Phaser( 1 + threads );

		final AtomicBoolean failed = new AtomicBoolean( false );

		for ( int th = 0; th < threads; th++ ) {
			Runnable t = () -> {

				startRunning.arriveAndAwaitAdvance();

				while ( true ) {

					final int i = currentRun.getAndIncrement();
					String s = i + containedString;

					if ( !cuckooFilter.insert( s ) ) {
						System.out.println( "Insert failed at: " + i );
						break;
					}
					if ( i > 100_000 && i % 2 == 0 ) {
						String c = ( i / 2 ) + containedString;
						if ( !cuckooFilter.contains( c ) ) {
							System.out.println( "Contains failed at: " + i );
							failed.set( true );
							break;
						}
						if ( !cuckooFilter.delete( c ) ) {
							System.out.println( "Delete failed at: " + i );
							failed.set( true );
							break;
						}

					}
				}

				stopRunning.arrive();
			};

			new Thread( t ).start();
		}

		startRunning.arriveAndAwaitAdvance();

		stopRunning.arriveAndAwaitAdvance();

		Assert.assertFalse( failed.get() );
	}

}
