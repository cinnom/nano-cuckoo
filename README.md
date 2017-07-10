nano-cuckoo
=====
Fast Java cuckoo filter implementation that utilizes sun.misc.Unsafe for native memory access.

What is a Cuckoo filter?
=====
["Cuckoo Filter: Better Than Bloom" by Bin Fan, David G. Andersen, Michael Kaminsky, and Michael D. Mitzenmacher](https://www.cs.cmu.edu/~dga/papers/cuckoo-conext2014.pdf)

[Probabilistic Filters By Example](https://bdupras.github.io/filter-tutorial/)

General Usage
=====
```java
import net.cinnom.nanocuckoo.NanoCuckooFilter;

public class CuckooTest {
	
    public void generalUsageTest() {
    
        long capacity = 32;
        
        // Use Builder to create a NanoCuckooFilter. Only required parameter is capacity.
        final NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( capacity )
                .withCountingEnabled( true ) // Enable counting
                .build();
        
        String testValue = "test value";
        
        // Insert a value into the filter
        cuckooFilter.insert( testValue );
        
        // Check that the value is in the filter
        boolean isValueInFilter = cuckooFilter.contains( testValue ); // Returns true
        
        // Check that some other value is in the filter
        isValueInFilter = cuckooFilter.contains( "some other value" ); // Should return false, probably
        
        // Insert the same value a couple more times
        cuckooFilter.insert( testValue );
        cuckooFilter.insert( testValue );
        
        // Get a count of how many times the value is in the filter
        int insertedCount = cuckooFilter.count( testValue ); // Returns 3 since we inserted three times with counting enabled
        
        // Delete value from the filter once
        boolean wasDeleted = cuckooFilter.delete( testValue ); // Returns true since a value was deleted
        
        // Try to delete the value up to six more times
        int deletedCount = cuckooFilter.delete( testValue, 6 ); // Returns 2 since only two copies of the value were left
        
        isValueInFilter = cuckooFilter.contains( testValue ); // Returns false since all copies of the value were deleted
        
        // Double filter capacity by doubling entries per bucket. However, this also roughly doubles max FPP.
        cuckooFilter.expand();
        
        // Close the filter when finished with it. This is important!
        cuckooFilter.close();
    }
}
```

Serialization
=====
```java
import net.cinnom.nanocuckoo.NanoCuckooFilter;
import java.io.*;

public class CuckooTest {
	
    public void serializationTest() throws IOException, ClassNotFoundException {
    
        // Create a filter
        NanoCuckooFilter cuckooFilter = new NanoCuckooFilter.Builder( 32 ).build();
        
        String testValue = "test value";
        
        // Insert a value into the filter
        cuckooFilter.insert( testValue );
        
        // Serialize the filter to a byte array
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream( byteOutputStream );
        objectOutputStream.writeObject( cuckooFilter );
        byte[] serializedBytes = byteOutputStream.toByteArray();
        
        // Close the current filter before replacing it
        cuckooFilter.close();
        
        // Read the serialized filter in from the byte array
        ByteArrayInputStream byteInputStream = new ByteArrayInputStream( serializedBytes );
        ObjectInputStream objectInputStream = new ObjectInputStream( byteInputStream );
        cuckooFilter = (NanoCuckooFilter) objectInputStream.readObject();
        
        boolean isValueInFilter = cuckooFilter.contains( testValue ); // Returns true
        
        // Close the filter
        cuckooFilter.close();
    }
}
```

Configuration
=====
See [NanoCuckooFilter.Builder](https://cinnom.github.io/nano-cuckoo/index.html?net/cinnom/nanocuckoo/NanoCuckooFilter.Builder.html) documentation for various settings.

Type Support
=====
Currently, only String, byte[], and long can be inserted into the filter. Generic type support may be added later, but I generally recommend serializing data yourself for optimal performance. 

Multithreading
=====
Multithreaded insert/delete/contains/count are supported. Bucket expansion and serialization will lock all buckets until they are finished.