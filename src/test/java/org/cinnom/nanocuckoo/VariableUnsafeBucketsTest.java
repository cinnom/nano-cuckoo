package org.cinnom.nanocuckoo;

import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Created by rjones on 6/28/17.
 */
public class VariableUnsafeBucketsTest {

	@Test
	public void test() throws NoSuchFieldException, IllegalAccessException {

		Field singleoneInstanceField = Unsafe.class.getDeclaredField( "theUnsafe" );
		singleoneInstanceField.setAccessible( true );
		Unsafe unsafe = (Unsafe) singleoneInstanceField.get( null );

		long addresses = unsafe.allocateMemory( 16 );
		unsafe.setMemory( addresses, 16, (byte) 0 );

		System.out.println(unsafe.compareAndSwapInt( null, addresses, 0, 10 ));
		System.out.println(unsafe.compareAndSwapInt( null, addresses, 10, 11 ));
		System.out.println(unsafe.compareAndSwapInt( null, addresses, 10, 10 ));
	}

}
