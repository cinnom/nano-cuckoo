package net.cinnom.nanocuckoo;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;

public class UnsafeProviderTest {

	@Test
	public void notNullTest() {

		Assert.assertNotNull( new UnsafeProvider().getUnsafe() );
	}

	@Test
	public void noSuchFieldExceptionTest() throws NoSuchFieldException {

		final UnsafeProvider unsafeProvider = new UnsafeProvider();

		final UnsafeProvider spyUnsafeProvider = spy( unsafeProvider );

		doThrow( NoSuchFieldException.class ).when( spyUnsafeProvider ).getUnsafeField();

		try {
			spyUnsafeProvider.init();
			Assert.fail();
		} catch ( final RuntimeException ex ) {
			Assert.assertEquals( ex.getMessage(), "Failed to obtain Unsafe" );
		}
	}

	@Test
	public void illegalAccessExceptionTest() throws NoSuchFieldException, IllegalAccessException {

		final UnsafeProvider unsafeProvider = new UnsafeProvider();

		final UnsafeProvider spyUnsafeProvider = spy( unsafeProvider );
		final Field unsafeField = mock( Field.class );

		doReturn( unsafeField ).when( spyUnsafeProvider ).getUnsafeField();
		when( unsafeField.get( null ) ).thenThrow( IllegalAccessException.class );

		try {
			spyUnsafeProvider.init();
			Assert.fail();
		} catch ( final RuntimeException ex ) {
			Assert.assertEquals( ex.getMessage(), "Failed to obtain Unsafe" );
		}
	}
}
