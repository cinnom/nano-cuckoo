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
package net.cinnom.nanocuckoo;

import java.io.Serializable;

import net.cinnom.nanocuckoo.hash.FingerprintHasher;

/**
 * "Smart" bucket swapper. Uses FastSwapper until a specified load factor is hit, then uses ReliableSwapper.
 */
class SmartSwapper implements Swapper, Serializable {

	private static final long serialVersionUID = 1L;

	private final FastSwapper fastSwapper;
	private final ReliableSwapper reliableSwapper;
	private final long maxFastCount;
	private final UnsafeBuckets buckets;

	SmartSwapper( final KickedValues kickedValues, final BucketLocker bucketLocker, final UnsafeBuckets buckets,
				  final FingerprintHasher fpHasher, final int maxKicks, final int randomSeed, double smartInsertLoadFactor ) {

		fastSwapper = new FastSwapper( kickedValues, bucketLocker, buckets, fpHasher, maxKicks, randomSeed );
		reliableSwapper = new ReliableSwapper( kickedValues, bucketLocker, buckets, fpHasher, maxKicks, randomSeed );

		this.maxFastCount = (long) ( buckets.getCapacity() * smartInsertLoadFactor );
		this.buckets = buckets;
	}

	@Override
	public boolean swap( int fingerprint, long bucket ) {

		if ( buckets.getInsertedCount() > maxFastCount ) {
			return reliableSwapper.swap( fingerprint, bucket );
		}

		return fastSwapper.swap( fingerprint, bucket );
	}
}
