import org.cinnom.nanocuckoo.buckets.internal.ConfigurableUnsafeBuckets;
import org.junit.Test;

/**
 * Created by rjones on 6/28/17.
 */
public class ConfigurableUnsafeBucketsTest {

	@Test
	public void test() throws NoSuchFieldException, IllegalAccessException {

		int entries = 2;
		int capacity = 50000000;
		int maxEntries = 3;
		int fpBits = 7;

		ConfigurableUnsafeBuckets unsafeBuckets = new ConfigurableUnsafeBuckets( entries, capacity, maxEntries, fpBits, false );

		//unsafeBuckets.insert( 0, 101, true );

		boolean leave = false;

		/*for(int i = 1; i < 128; i++) {
			for(int b = 0; b < unsafeBuckets.getCapacity(); b++) {
				unsafeBuckets.insert( b, i, true );

				//unsafeBuckets.putValue( 0, b, i );

				if(!unsafeBuckets.contains(b, i)) {
				//if(unsafeBuckets.getValue( 0, b ) != i) {
					System.out.println(b);
					System.out.println(i);
					System.out.println(unsafeBuckets.getValue( 0, b ));
					leave = true;
					break;
				}
			}
			if(leave) {
				break;
			}
		}*/

		unsafeBuckets.putValue( 0, 0, 127 );
		//unsafeBuckets.putValue( 0, 1, 0 );
		//unsafeBuckets.putValue( 0, 2, 127 );
		//unsafeBuckets.putValue( 0, 3, 127 );
		//unsafeBuckets.putValue( 0, 4, 127 );
		//unsafeBuckets.putValue( 0, 5, 0 );
		//unsafeBuckets.putValue( 0, 6, 0 );

		System.out.println(unsafeBuckets.getValue( 0, 0 ));
		System.out.println(unsafeBuckets.getValue( 0, 5 ));
		System.out.println(unsafeBuckets.getValue( 0, 6 ));
	}

}
