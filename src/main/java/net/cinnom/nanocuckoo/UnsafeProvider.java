package net.cinnom.nanocuckoo;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * Gives access to {@link Unsafe}.
 */
public class UnsafeProvider {

	private Unsafe unsafe;

	/**
	 * Instantiate an UnsafeProvider. Will try to get an {@link Unsafe}, and will throw a RuntimeException on failure.
	 */
	public UnsafeProvider() {

		init();
	}

	void init() {

		try {
			Field unsafeField = getUnsafeField();
			unsafeField.setAccessible( true );
			unsafe = (Unsafe) unsafeField.get( null );
		} catch ( final Throwable e ) {
			throw new RuntimeException( "Failed to obtain Unsafe", e );
		}
	}

	/**
	 * Get an instance of {@link Unsafe}.
	 */
	public Unsafe getUnsafe() {

		return unsafe;
	}

	Field getUnsafeField() throws NoSuchFieldException {

		return Unsafe.class.getDeclaredField( "theUnsafe" );
	}
}
