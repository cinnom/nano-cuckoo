import bloom.UnsafeBitArray;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by rjones on 6/26/17.
 */
public class UnsafeBitArrayTest {

	@Test
	public void checkSetBit() {

		long size = 16;

		try {
			UnsafeBitArray unsafeBitArray = new UnsafeBitArray( size );

			for(int i = 0; i < size; i++){

				unsafeBitArray.setBit( i );
				Assert.assertTrue( unsafeBitArray.hasBit( i ) );
			}

		} catch ( NoSuchFieldException e ) {
			e.printStackTrace();
		} catch ( IllegalAccessException e ) {
			e.printStackTrace();
		}
	}

	@Test
	public void checkSetBitWrong() {

		long size = 16;

		try {
			UnsafeBitArray unsafeBitArray = new UnsafeBitArray( size );

			for(int i = 1; i < size; i++){

				unsafeBitArray.setBit( i-1 );
				Assert.assertFalse( unsafeBitArray.hasBit( i ) );
			}

		} catch ( NoSuchFieldException e ) {
			e.printStackTrace();
		} catch ( IllegalAccessException e ) {
			e.printStackTrace();
		}
	}

}
