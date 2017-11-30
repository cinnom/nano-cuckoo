package net.cinnom.nanocuckoo;

import static net.jpountz.util.Utils.NATIVE_BYTE_ORDER;

import java.lang.reflect.Field;
import java.nio.ByteOrder;

import net.jpountz.util.UnsafeUtils;
import sun.misc.Unsafe;

/**
 * Gives access to {@link Unsafe}.
 */
public class UnsafeProvider {

	private Unsafe unsafe;
	private long stringValueFieldOffset = -1;
	private long charArrayOffset = -1;

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
			stringValueFieldOffset = unsafe.objectFieldOffset( String.class.getDeclaredField( "value" ) );
			charArrayOffset = unsafe.arrayBaseOffset( char[].class );
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

	public char[] getStringCharArray( final String string ) {

		return (char[]) unsafe.getObject( string, stringValueFieldOffset );
	}

	public short readShort( char[] src, int srcOff ) {

		return unsafe.getShort( src, charArrayOffset + srcOff );
	}

	public int readShortLE( char[] src, int srcOff ) {

		short s = readShort( src, srcOff );
		if ( NATIVE_BYTE_ORDER == ByteOrder.BIG_ENDIAN ) {
			s = Short.reverseBytes( s );
		}
		return s & 0xFFFF;
	}
}
