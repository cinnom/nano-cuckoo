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

import org.junit.Assert;
import org.junit.Test;

/**
 * KickedValues tests.
 */
public class KickedValuesTest {

	@Test
	public void setGetFingerprintTest() {

		KickedValues kickedValues = new KickedValues();

		int kickedFingerprint = 1342645;

		kickedValues.setKickedFingerprint( kickedFingerprint );
		Assert.assertEquals( kickedFingerprint, kickedValues.getKickedFingerprint() );
	}

	@Test
	public void compareAndSetFingerprintTest() {

		KickedValues kickedValues = new KickedValues();

		int kickedFingerprint = 1342645;

		Assert.assertTrue( kickedValues.compareAndSetKickedFingerprint( kickedFingerprint ) );
		Assert.assertEquals( kickedFingerprint, kickedValues.getKickedFingerprint() );
	}

	@Test
	public void compareAndSetFalseFingerprintTest() {

		KickedValues kickedValues = new KickedValues();

		int kickedFingerprint = 1342645;
		int kickedFingerprint2 = 6456454;

		kickedValues.setKickedFingerprint( kickedFingerprint );

		Assert.assertFalse( kickedValues.compareAndSetKickedFingerprint( kickedFingerprint2 ) );
		Assert.assertEquals( kickedFingerprint, kickedValues.getKickedFingerprint() );
	}

	@Test
	public void setGetBucketTest() {

		KickedValues kickedValues = new KickedValues();

		long kickedBucket = 134264513423L;

		kickedValues.setKickedBucket( kickedBucket );
		Assert.assertEquals( kickedBucket, kickedValues.getKickedBucket() );
	}

	@Test
	public void equalsBucket1Test() {

		KickedValues kickedValues = new KickedValues();

		long kickedBucket = 134264513423L;
		long kickedBucket2 = 5434343367311L;
		int kickedFingerprint = 1342645;

		kickedValues.setKickedFingerprint( kickedFingerprint );
		kickedValues.setKickedBucket( kickedBucket );

		Assert.assertTrue( kickedValues.equals( kickedFingerprint, kickedBucket, kickedBucket2 ) );
	}

	@Test
	public void equalsBucket2Test() {

		KickedValues kickedValues = new KickedValues();

		long kickedBucket = 134264513423L;
		long kickedBucket2 = 5434343367311L;
		int kickedFingerprint = 1342645;

		kickedValues.setKickedFingerprint( kickedFingerprint );
		kickedValues.setKickedBucket( kickedBucket2 );

		Assert.assertTrue( kickedValues.equals( kickedFingerprint, kickedBucket, kickedBucket2 ) );
	}

	@Test
	public void notEqualsFingerprintTest() {

		KickedValues kickedValues = new KickedValues();

		long kickedBucket = 134264513423L;
		long kickedBucket2 = 5434343367311L;
		int kickedFingerprint = 1342645;

		kickedValues.setKickedFingerprint( kickedFingerprint );
		kickedValues.setKickedBucket( kickedBucket );

		Assert.assertFalse( kickedValues.equals( 75642, kickedBucket, kickedBucket2 ) );
	}

	@Test
	public void notEqualsBucketsTest() {

		KickedValues kickedValues = new KickedValues();

		long kickedBucket = 134264513423L;
		long kickedBucket2 = 5434343367311L;
		int kickedFingerprint = 1342645;

		kickedValues.setKickedFingerprint( kickedFingerprint );
		kickedValues.setKickedBucket( kickedBucket );

		Assert.assertFalse( kickedValues.equals( kickedFingerprint, 5463434, 567334 ) );
	}

	@Test
	public void clearTest() {

		KickedValues kickedValues = new KickedValues();

		long kickedBucket = 134264513423L;
		long kickedBucket2 = 5434343367311L;
		int kickedFingerprint = 1342645;

		kickedValues.setKickedFingerprint( kickedFingerprint );
		kickedValues.setKickedBucket( kickedBucket );

		Assert.assertFalse( kickedValues.isClear() );

		kickedValues.clear();

		Assert.assertTrue( kickedValues.isClear() );
		Assert.assertEquals( -1, kickedValues.getKickedBucket() );
		Assert.assertEquals( -1, kickedValues.getKickedFingerprint() );
	}

	@Test
	public void lockUnlockTest() {

		KickedValues kickedValues = new KickedValues();

		kickedValues.lock();
		kickedValues.unlock();
	}

	@Test
	public void unlockFailsTest() {

		KickedValues kickedValues = new KickedValues();

		try {
			kickedValues.unlock();
			Assert.fail();
		} catch ( IllegalMonitorStateException e ) {

		}
	}
}
