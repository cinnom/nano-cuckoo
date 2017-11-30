package net.cinnom.nanocuckoo.dumb;

import java.io.Serializable;

import net.cinnom.nanocuckoo.encode.StringEncoder;

public class DumbStringEncoder implements StringEncoder, Serializable {

	@Override
	public byte[] encode( final String data ) {

		return new byte[] { 127 };
	}

}
