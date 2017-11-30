package net.cinnom.nanocuckoo.dumb;

import java.io.Serializable;

import net.cinnom.nanocuckoo.random.RandomInt;

public class DumbRandomInt implements RandomInt, Serializable {

	@Override
	public int nextInt() {

		return 0;
	}

}
