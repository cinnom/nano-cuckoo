package net.cinnom.nanocuckoo;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import net.cinnom.nanocuckoo.hash.FingerprintHasher;

/**
 * FastSwapper tests.
 */
public class ReliableSwapperTest {

	@Test
	public void basicSwapInsertTest() {

		final KickedValues kickedValues = new KickedValues();
		final BucketLocker bucketLocker = mock( BucketLocker.class );
		final UnsafeBuckets unsafeBuckets = mock( UnsafeBuckets.class );
		final FingerprintHasher fingerprintHasher = mock( FingerprintHasher.class );
		final int maxKicks = 400;
		final int randomSeed = 0x48F7E28A;

		final int fingerprint = 5;
		final long bucket = 37;

		when( unsafeBuckets.getEntryMask() ).thenReturn( 3 );
		when( unsafeBuckets.swap( anyInt(), eq( bucket ), eq( fingerprint ) ) ).thenReturn( 117 );
		when( fingerprintHasher.getHash( 117 ) ).thenReturn( 123456789L );
		when( unsafeBuckets.getBucket( 123456789L )).thenReturn( 12L );
		when( unsafeBuckets.insert( 41, 117 ) ).thenReturn( true );

		final ReliableSwapper reliableSwapperSwapper = new ReliableSwapper( kickedValues, bucketLocker, unsafeBuckets, fingerprintHasher, maxKicks, randomSeed );

		reliableSwapperSwapper.swap( fingerprint, bucket );

		verify( unsafeBuckets ).insert( 41, 117 );
	}
}
