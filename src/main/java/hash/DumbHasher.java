package hash;

import java.lang.reflect.Field;
import java.util.SplittableRandom;

import random.Randomizer;
import sun.misc.Unsafe;

/**
 * Created by rjones on 6/29/17.
 */
public class DumbHasher implements FingerprintHasher, Randomizer {

	private static final int POS_MASK = 0x7FFFFFFF;

	private final Unsafe unsafe;
	private final SplittableRandom random;

	private final long address;
	private final long capacity;
	private final long capacityMask;

	private long randomLol = 0;

	public DumbHasher( int width, int seed ) throws NoSuchFieldException, IllegalAccessException {

		random = new SplittableRandom( seed );

		Field singleoneInstanceField = Unsafe.class.getDeclaredField( "theUnsafe" );
		singleoneInstanceField.setAccessible( true );
		unsafe = (Unsafe) singleoneInstanceField.get( null );

		int capacity = (int) Math.pow( 2, width );
		capacityMask = capacity - 1;

		int capacityBytes = capacity + 7;

		address = unsafe.allocateMemory( capacityBytes );
		unsafe.setMemory( address, capacityBytes, (byte) 0 );

		int i;
		for ( i = 0; i < capacity; i += 8 ) {

			unsafe.putLong( address + i, random.nextLong() );
		}

		unsafe.putLong( address + ( capacity - 1 ), random.nextLong() );

		this.capacity = capacity;
	}

	@Override
	public long getHash( int value ) {

		return unsafe.getLong( address + value );
	}

	@Override
	public int nextInt( ) {

		return unsafe.getInt( address + ( randomLol++ & capacityMask ) );
	}
}
