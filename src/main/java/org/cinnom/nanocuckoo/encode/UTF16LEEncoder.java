package org.cinnom.nanocuckoo.encode;

/**
 * StringEncoder for encoding UTF-16 Little Endian Strings.
 */
public class UTF16LEEncoder implements StringEncoder {

    /**
     * Encode a String into UTF-16 Little Endian bytes.
     *
     * @param data String to encode.
     * @return UTF-16 Little Endian bytes.
     */
    @Override
    public byte[] encode(final String data) {

        final int length = data.length();
        final byte b[] = new byte[length * 2];

        for (int j = 0; j < length; j++) {

            int offset = j << 1;
            int nextOffset = offset + 1;
            char charAtJ = data.charAt(j);

            b[offset] = (byte) (charAtJ & 0xFF);
            b[nextOffset] = (byte) (charAtJ >> 8);
        }

        return b;
    }

}
