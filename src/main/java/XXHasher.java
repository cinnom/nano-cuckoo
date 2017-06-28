import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by rjones on 6/22/17.
 */
public class XXHasher {

	private static final XXHashFactory factory = XXHashFactory.fastestInstance();

	private static int seed = 0x48f7e28a;

	public static long getHash( final byte[] data ) {

		return factory.hash64().hash( data, 0, data.length, seed );
	}

	public static long getHash( final int data ) {

		// Transform int to BE byte array
		final byte[] buf = new byte[] {
				(byte)(data >>> 24),
				(byte)(data >>> 16),
				(byte)(data >>> 8),
				(byte)data};

		return factory.hash64().hash( buf, 0, 4, seed );
	}

}
