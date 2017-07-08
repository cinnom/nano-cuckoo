nano-cuckoo
=====
Fast Java cuckoo filter implementation that utilizes sun.misc.Unsafe for native memory access.

What is a Cuckoo filter?
=====
["Cuckoo Filter: Better Than Bloom" by Bin Fan, David G. Andersen, Michael Kaminsky, and Michael D. Mitzenmacher](https://www.cs.cmu.edu/~dga/papers/cuckoo-conext2014.pdf)

[Probabilistic Filters By Example](https://bdupras.github.io/filter-tutorial/)

Quick Start
=====
```java
import org.cinnom.nanocuckoo.NanoCuckooFilter;

public class CuckooTest {
	
	public void test() {
		
		int capacity = 100000000;
		
		// Use Builder to create a NanoCuckooFilter. Only required parameter is capacity.
		NanoCuckooFilter filter = new NanoCuckooFilter.Builder( capacity ).build();
		
		String testValue = "test value";
		
		// Insert a value into the filter
		filter.insert( testValue );
		
		// Check that the value is in the filter
		boolean isValueInFilter = filter.contains( testValue ); // should return true
		
		// Delete value from the filter
		filter.delete( testValue );
		
		isValueInFilter = filter.contains( testValue ); // should return false
	}
}
```

Configuration
=====
