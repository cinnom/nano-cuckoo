package org.cinnom.nanocuckoo;

import org.cinnom.nanocuckoo.NanoCuckooFilter;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by rjones on 6/26/17.
 */
public class NanoCuckooFilterTest {

	@Test
	public void insertTest() throws InterruptedException {

		final int threads = 16;
		int entryBits = 2;
		int capacity = 25000000;

		NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( capacity ).withFingerprintBits( 8 ).build();

		int entries = (int) Math.pow( 2, entryBits );
		long fullCapacity = cuckooFilter.getCapacity() * entries;

		long runs = 2 * fullCapacity;

		System.out.println( cuckooFilter.getMemoryUsageBytes() );
		System.out.println( cuckooFilter.getCapacity() );

		final AtomicInteger actualRunsA = new AtomicInteger(  );
		final AtomicInteger runningThreads = new AtomicInteger( threads );

		long currentMillis1 = System.currentTimeMillis();

		for ( int th = 0; th < threads; th++ ) {
			Runnable t = () -> {

				while(true) {

					int i = actualRunsA.getAndIncrement();
					String s = i + "abcdefghijklmn-opqrstuvwxyz-000000";

					if ( !cuckooFilter.insert( s ) ) {
						System.out.println( "Insert failed at: " + i );
						break;
					}
				}

				runningThreads.decrementAndGet();
			};

			new Thread(t).start();
		}

		while(runningThreads.get() != 0) {
			Thread.sleep( 1 );
		}

		long currentMillis2 = System.currentTimeMillis();

		final int actualRuns = actualRunsA.getAndSet(0);

		System.out.println( "Insert ops/sec: " + ( actualRuns / ( ( currentMillis2 - currentMillis1 ) / 1000 ) ) );

		System.out.println( "Duplicates: " + cuckooFilter.getDuplicates() );
		System.out.println(
				"LF: " + ( (double) actualRuns - (double) cuckooFilter.getDuplicates() ) / (double) fullCapacity );

		runningThreads.set( threads );

		for ( int th = 0; th < threads; th++ ) {
			Runnable t = () -> {

				while(true) {

					int i = actualRunsA.getAndIncrement();
					String s = i + "abcdefghijklmn-opqrstuvwxyz-000000";

					if ( !cuckooFilter.contains( s ) ) {
						System.out.println( "Contains failed at: " + i );
						break;
					}
				}

				runningThreads.decrementAndGet();
			};

			new Thread(t).start();
		}

		while(runningThreads.get() != 0) {
			Thread.sleep( 1 );
		}

		long currentMillis3 = System.currentTimeMillis();
		System.out.println(
				"Contains (true) ops/sec: " + ( actualRuns / ( ( currentMillis3 - currentMillis2 ) / 1000 ) ) );

		final AtomicInteger falsePos = new AtomicInteger(  );

		actualRunsA.getAndSet(0);
		runningThreads.set( threads );

		for ( int th = 0; th < threads; th++ ) {
			Runnable t = () -> {

				while(true) {

					int i = actualRunsA.getAndIncrement();

					if(i >= actualRuns) {
						break;
					}

					String s = i + "1abcdefghijklm1";

					if ( cuckooFilter.contains( s ) ) {
						falsePos.incrementAndGet();
					}
				}

				runningThreads.decrementAndGet();
			};

			new Thread(t).start();
		}

		while(runningThreads.get() != 0) {
			Thread.sleep( 1 );
		}

		long currentMillis4 = System.currentTimeMillis();
		System.out.println(
				"Contains (false) ops/sec: " + ( actualRuns / ( ( currentMillis4 - currentMillis3 ) / 1000 ) ) );

		System.out.println( "FPP: " + (double) falsePos.get() / (double) actualRuns );

	}

}
