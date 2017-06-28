package buckets;

/**
 * Created by rjones on 6/28/17.
 */
public interface Buckets {

	long getMemoryUsageBytes();

	long getBucket( long hash );

	boolean expand();

	boolean contains( long bucket, int value );

	boolean insert( long bucket, int value, boolean noDuplicate );

	int swap( int entry, long bucket, int value );

	int getEntries();

	long getCapacity();

	void delete();
}
