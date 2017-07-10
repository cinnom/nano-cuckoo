package net.cinnom.nanocuckoo;

import net.cinnom.nanocuckoo.hash.FingerprintHasher;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FastSwapper tests.
 */
public class FastSwapperTest {

	@Test
	public void basicSwapInsertTest() {

		final KickedValues kickedValues = mock( KickedValues.class );
		final BucketLocker bucketLocker = mock( BucketLocker.class );
		final UnsafeBuckets unsafeBuckets = mock( UnsafeBuckets.class );
		final FingerprintHasher fingerprintHasher = mock( FingerprintHasher.class );
		final int maxKicks = 400;
		final int randomSeed = 0x48F7E28A;

		final int fingerprint = 5;
		final long bucket = 37;

		when( kickedValues.isClear() ).thenReturn( true );
		when( unsafeBuckets.getEntryMask() ).thenReturn( 3 );
		when( unsafeBuckets.swap( anyInt(), eq( bucket ), eq( fingerprint ) ) ).thenReturn( 117 );
		when( fingerprintHasher.getHash( 117 ) ).thenReturn( 123456789L );
		when( unsafeBuckets.getBucket( 123456789L )).thenReturn( 12L );
		when( unsafeBuckets.insert( 41, 117 ) ).thenReturn( true );

		final FastSwapper fastSwapper = new FastSwapper( kickedValues, bucketLocker, unsafeBuckets, fingerprintHasher, maxKicks, randomSeed );

		fastSwapper.swap( fingerprint, bucket );

		verify( unsafeBuckets ).insert( 41, 117 );
	}
}
