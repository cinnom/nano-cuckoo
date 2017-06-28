package bloom;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Created by rjones on 6/26/17.
 */
public class UnsafeBitArray {

	private final Unsafe unsafe;
	private long address;

	private final long capacity;

	public UnsafeBitArray(long size) throws NoSuchFieldException, IllegalAccessException {

		if(size % 8 != 0) {
			size += 8;
		}

		Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
		singleoneInstanceField.setAccessible(true);
		unsafe = (Unsafe) singleoneInstanceField.get(null);

		address = unsafe.allocateMemory( size / 8 );
		unsafe.setMemory( address, size / 8, (byte) 0 );

		this.capacity = size;
	}

	public void setBit(long bit) {

		long bitMultiple = bit / 8;
		long bitPos = (long) Math.pow(2, bit % 8);

		byte modifyByte = unsafe.getByte( address + bitMultiple );
		modifyByte |= bitPos;

		unsafe.putByte( address + bitMultiple, modifyByte );
	}

	public boolean hasBit(long bit) {

		long bitMultiple = bit / 8;
		long bitPos = (long) Math.pow(2, bit % 8);

		byte modifyByte = unsafe.getByte( address + bitMultiple );
		modifyByte &= bitPos;

		return modifyByte != 0;
	}

	public byte getByte(long b) {
		return unsafe.getByte( address + b );
	}

}
