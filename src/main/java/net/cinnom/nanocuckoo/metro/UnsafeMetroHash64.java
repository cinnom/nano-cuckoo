/*
 * Copyright 2017 Randall Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.cinnom.nanocuckoo.metro;

import net.jpountz.util.UnsafeUtils;

/**
 * Unsafe MetroHash 64-bit implementation. Thread-safe.
 *
 * @see <a href="http://www.jandrewrogers.com/2015/05/27/metrohash/">MetroHash: Faster, Better Hash Functions</a>
 */
public class UnsafeMetroHash64 {

	private static final long K0 = 0xD6D018F5L;
	private static final long K1 = 0xA2AA033BL;
	private static final long K2 = 0x62992FC1L;
	private static final long K3 = 0x30BC5B29L;

	public UnsafeMetroHash64() {

	}

	/**
	 * Compute a 64-bit hash.
	 * 
	 * @param buf
	 *            Input buffer.
	 * @param off
	 *            Offset to start reading.
	 * @param len
	 *            Number of bytes to read.
	 * @param seed
	 *            Hash seed.
	 * @return 64-bit hash.
	 */
	public long hash( byte[] buf, int off, int len, long seed ) {

		boolean filled = false;
		long hash = ( seed + K2 ) * K0;
		long v0 = hash;
		long v1 = hash;
		long v2 = hash;
		long v3 = hash;
		int remaining = len - off;

		if ( remaining >= 32 ) {
			filled = true;
			do {
				v0 += UnsafeUtils.readLongLE( buf, off ) * K0;
				v0 = Long.rotateRight( v0, 29 ) + v2;
				off += 8;
				v1 += UnsafeUtils.readLongLE( buf, off ) * K1;
				v1 = Long.rotateRight( v1, 29 ) + v3;
				off += 8;
				v2 += UnsafeUtils.readLongLE( buf, off ) * K2;
				v2 = Long.rotateRight( v2, 29 ) + v0;
				off += 8;
				v3 += UnsafeUtils.readLongLE( buf, off ) * K3;
				v3 = Long.rotateRight( v3, 29 ) + v1;
				off += 8;
				remaining -= 32;
			} while ( remaining >= 32 );
		}

		if ( filled ) {
			v2 ^= Long.rotateRight( ( ( v0 + v3 ) * K0 ) + v1, 37 ) * K1;
			v3 ^= Long.rotateRight( ( ( v1 + v2 ) * K1 ) + v0, 37 ) * K0;
			v0 ^= Long.rotateRight( ( ( v0 + v2 ) * K0 ) + v3, 37 ) * K1;
			v1 ^= Long.rotateRight( ( ( v1 + v3 ) * K1 ) + v2, 37 ) * K0;
			hash += v0 ^ v1;
		}
		if ( remaining >= 16 ) {
			v0 = hash + UnsafeUtils.readLongLE( buf, off ) * K2;
			v0 = Long.rotateRight( v0, 29 ) * K3;
			off += 8;
			v1 = hash + UnsafeUtils.readLongLE( buf, off ) * K2;
			v1 = Long.rotateRight( v1, 29 ) * K3;
			off += 8;
			v0 ^= Long.rotateRight( v0 * K0, 21 ) + v1;
			v1 ^= Long.rotateRight( v1 * K3, 21 ) + v0;
			hash += v1;
			remaining -= 16;
		}
		if ( remaining >= 8 ) {
			hash += UnsafeUtils.readLongLE( buf, off ) * K3;
			hash ^= Long.rotateRight( hash, 55 ) * K1;
			off += 8;
			remaining -= 8;
		}
		if ( remaining >= 4 ) {
			hash += UnsafeUtils.readIntLE( buf, off ) * K3;
			hash ^= Long.rotateRight( hash, 26 ) * K1;
			off += 4;
			remaining -= 4;
		}
		if ( remaining >= 2 ) {
			hash += UnsafeUtils.readShortLE( buf, off ) * K3;
			hash ^= Long.rotateRight( hash, 48 ) * K1;
			off += 2;
			remaining -= 2;
		}
		if ( remaining >= 1 ) {
			hash += UnsafeUtils.readByte( buf, off ) * K3;
			hash ^= Long.rotateRight( hash, 37 ) * K1;
		}
		hash ^= Long.rotateRight( hash, 28 );
		hash *= K0;
		hash ^= Long.rotateRight( hash, 29 );
		return hash;
	}
}
