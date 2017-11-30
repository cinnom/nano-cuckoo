package net.cinnom.nanocuckoo;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class UnsafeProvider {

	public Unsafe getUnsafe() {

		try {
			Field unsafeField = getUnsafeField();
			unsafeField.setAccessible( true );
			return (Unsafe) unsafeField.get( null );
		} catch ( IllegalAccessException | NoSuchFieldException e ) {
			throw new RuntimeException( "Failed to obtain Unsafe", e );
		}
	}

	Field getUnsafeField() throws NoSuchFieldException {

		return Unsafe.class.getDeclaredField( "theUnsafe" );
	}
}
