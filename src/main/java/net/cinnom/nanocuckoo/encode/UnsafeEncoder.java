package net.cinnom.nanocuckoo.encode;

import java.lang.reflect.Field;

import net.cinnom.nanocuckoo.UnsafeProvider;
import sun.misc.Unsafe;

/**
 * StringEncoder that will directly copy char[] to byte[] using {@link Unsafe}. Note that endianness of the resulting
 * byte[] will be system-dependent; this encoder should not be used across platforms that store characters in different
 * formats.
 * <p>
 * Performance on this encoder is better than all other encoders for Strings around 40 characters or more.
 * </p>
 */
public class UnsafeEncoder implements StringEncoder {

	private Unsafe unsafe;
	private long stringValueFieldOffset;
	private long charArrayOffset;
	private long byteArrayOffset;

	/**
	 * Instantiate an UnsafeEncoder. Will try to get an {@link Unsafe} as well as field offsets, and will throw a
	 * RuntimeException on failure.
	 */
	public UnsafeEncoder() {

		init();
	}

	void init() {

		try {
			unsafe = new UnsafeProvider().getUnsafe();
			stringValueFieldOffset = unsafe.objectFieldOffset( getStringValueField() );
			charArrayOffset = unsafe.arrayBaseOffset( char[].class );
			byteArrayOffset = unsafe.arrayBaseOffset( byte[].class );
		} catch ( NoSuchFieldException ex ) {
			throw new RuntimeException( "Failed to get field offsets", ex );
		}
	}

	Field getStringValueField() throws NoSuchFieldException {

		return String.class.getDeclaredField( "value" );
	}

	/**
	 * Encode a String into UTF-16 with system-dependent endianness.
	 *
	 * @param data
	 *            String to encode.
	 * @return UTF-16 LE or BE bytes.
	 */
	@Override
	public byte[] encode( final String data ) {

		final char[] src = (char[]) unsafe.getObject( data, stringValueFieldOffset );
		final byte[] out = new byte[src.length << 1];

		unsafe.copyMemory( src, charArrayOffset, out, byteArrayOffset, out.length );
		return out;
	}
}
