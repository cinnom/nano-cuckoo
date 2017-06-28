/**
 * Created by rjones on 6/27/17.
 */
public class QuickLogger {

	private static boolean enabled = false;

	public static void log( String message ) {

		if(enabled) {
			System.out.println( message );
		}
	}

}
