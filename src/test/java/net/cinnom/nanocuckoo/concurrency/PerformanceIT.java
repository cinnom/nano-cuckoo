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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import net.cinnom.nanocuckoo.NanoCuckooFilter;

/**
 * Informal performance/multithreading test.
 */
public class PerformanceIT {

	@Test
	public void perfTest() throws InterruptedException, IOException, ClassNotFoundException {

		final int threads = 6;
		final int capacity = 10_000_000;
		final String containedString = "abcdefghijklmn-opqrstuvwxyz-000000";
		final String notContainedString = "abcdefghijklmn-opqrstuvwxyz-111111";

		final NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( capacity ).withCountingEnabled( true )
				.withFingerprintBits( 7 ).build();

		System.out.println( "Memory usage bytes: " + cuckooFilter.getMemoryUsageBytes() );
		System.out.println( "Capacity: " + cuckooFilter.getCapacity() );

		final AtomicInteger currentRun = new AtomicInteger();

		// Set phasers to stun
		final Phaser startRunning = new Phaser( 1 + threads );
		final Phaser stopRunning = new Phaser( 1 + threads );

		for ( int th = 0; th < threads; th++ ) {
			Runnable t = () -> {

				startRunning.arriveAndAwaitAdvance();

				while ( true ) {

					int i = currentRun.getAndIncrement();
					String s = i + containedString;

					if ( !cuckooFilter.insert( s ) ) {
						System.out.println( "Insert failed at: " + i );
						break;
					}
				}

				stopRunning.arrive();
			};

			new Thread( t ).start();
		}

		startRunning.arriveAndAwaitAdvance();
		long startNanos = System.nanoTime();

		Thread.sleep( 1000 );
		cuckooFilter.expand(); // Expand in the middle of doing inserts

		stopRunning.arriveAndAwaitAdvance();

		long timeNanos = Math.max( ( System.nanoTime() - startNanos ) / 1_000_000_000, 1 );

		final int actualRuns = currentRun.getAndSet( 0 );

		System.out.println( "Memory usage bytes: " + cuckooFilter.getMemoryUsageBytes() );
		System.out.println( "Capacity: " + cuckooFilter.getCapacity() );
		System.out.println( "Insert ops/sec: " + ( actualRuns / timeNanos ) );
		System.out.println( "Load Factor: " + cuckooFilter.getLoadFactor() );

		for ( int th = 0; th < threads; th++ ) {
			Runnable t = () -> {

				startRunning.arriveAndAwaitAdvance();

				while ( true ) {

					int i = currentRun.getAndIncrement();
					String s = i + containedString;

					if ( !cuckooFilter.contains( s ) ) {
						System.out.println( "Contains failed at: " + i );
						break;
					}
				}

				stopRunning.arrive();
			};

			new Thread( t ).start();
		}

		startRunning.arriveAndAwaitAdvance();
		startNanos = System.nanoTime();
		stopRunning.arriveAndAwaitAdvance();

		timeNanos = Math.max( ( System.nanoTime() - startNanos ) / 1_000_000_000, 1 );

		System.out.println( "Contains (true) ops/sec: " + ( actualRuns / timeNanos ) );

		final AtomicInteger falsePos = new AtomicInteger();

		currentRun.set( 0 );

		for ( int th = 0; th < threads; th++ ) {
			Runnable t = () -> {

				startRunning.arriveAndAwaitAdvance();

				while ( true ) {

					int i = currentRun.getAndIncrement();

					if ( i >= actualRuns ) {
						break;
					}

					String s = i + notContainedString;

					if ( cuckooFilter.contains( s ) ) {
						falsePos.incrementAndGet();
					}
				}

				stopRunning.arrive();
			};

			new Thread( t ).start();
		}

		startRunning.arriveAndAwaitAdvance();
		startNanos = System.nanoTime();
		stopRunning.arriveAndAwaitAdvance();

		timeNanos = Math.max( ( System.nanoTime() - startNanos ) / 1_000_000_000, 1 );

		System.out.println( "Contains (false) ops/sec: " + ( actualRuns / timeNanos ) );
		System.out.println( "FPP: " + (double) falsePos.get() / (double) actualRuns );

		currentRun.set( 0 );

		for ( int th = 0; th < threads; th++ ) {
			Runnable t = () -> {

				startRunning.arriveAndAwaitAdvance();

				while ( true ) {

					int i = currentRun.getAndIncrement();
					String s = i + containedString;

					if ( !cuckooFilter.delete( s ) ) {
						System.out.println( "Delete failed at: " + i );
						break;
					}
				}

				stopRunning.arrive();
			};

			new Thread( t ).start();
		}

		startRunning.arriveAndAwaitAdvance();
		startNanos = System.nanoTime();
		stopRunning.arriveAndAwaitAdvance();

		timeNanos = Math.max( ( System.nanoTime() - startNanos ) / 1_000_000_000, 1 );

		System.out.println( "Delete ops/sec: " + ( actualRuns / timeNanos ) );
	}

}
