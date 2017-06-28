import org.junit.Test;

import java.io.UnsupportedEncodingException;

/**
 * Created by rjones on 6/26/17.
 */
public class CuckooFilterTest {

	@Test
	public void insertTest() {

		int entries = 2;
		int capacity = 50000000;
		int maxEntries = 3;
		int fpBits = 7;

		try {
			CuckooFilter cuckooFilter = new CuckooFilter( entries, capacity, true, maxEntries, fpBits );

			int runs = 100000000;

			System.out.println( cuckooFilter.getMemoryUsageBytes() );
			System.out.println( cuckooFilter.getCapacity() );


			long currentMillis1 = System.currentTimeMillis();
			for(int i = 0; i < runs; i++) {

				String s = i + "abcdefghijklmn-opqrstuvwxyz-000000";
				byte[] bytes = s.getBytes( "UTF-8" );

				if( !cuckooFilter.insert( bytes ) ) {
					System.out.println( "Insert failed at: " + i );
					break;
				}
			}
			long currentMillis2 = System.currentTimeMillis();
			System.out.println("Insert ops/sec: " + (runs / ((currentMillis2 - currentMillis1) / 1000)) );

			for(int i = 0; i < runs; i++) {

				String s = i + "abcdefghijklmn-opqrstuvwxyz-000000";
				byte[] bytes = s.getBytes( "UTF-8" );

				if( !cuckooFilter.contains( bytes ) ) {
					System.out.println( "Contains failed at: " + i );
					break;
				}
			}

			long currentMillis3 = System.currentTimeMillis();
			System.out.println("Contains (true) ops/sec: " + (runs / ((currentMillis3 - currentMillis2) / 1000)) );

			long falsePos = 0;

			for(int i = 0; i < runs; i++) {

				String s = i + "1abcdefghijklm1";
				byte[] bytes = s.getBytes( "UTF-8" );

				if( cuckooFilter.contains( bytes ) ) {
					falsePos++;
				}
			}

			long currentMillis4 = System.currentTimeMillis();
			System.out.println("Contains (false) ops/sec: " + (runs / ((currentMillis4 - currentMillis3) / 1000)) );

			System.out.println("FPP: " + (double)falsePos / (double)runs);

		} catch ( NoSuchFieldException e ) {
			e.printStackTrace();
		} catch ( IllegalAccessException e ) {
			e.printStackTrace();
		} catch ( UnsupportedEncodingException e ) {
			e.printStackTrace();
		}
	}

}
