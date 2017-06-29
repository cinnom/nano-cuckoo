package hash;

/**
 * Created by rjones on 6/29/17.
 */
public interface BucketHasher {

	long getHash( final byte[] data );

}
