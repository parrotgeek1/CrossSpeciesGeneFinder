public class MyTime {
	private static boolean debugNoWait = false;
	
	public static void sleep(long time) {
		if(!debugNoWait) sleepAlways(time);
	}
	public static void sleepAlways(long time) {
		try {
			Thread.sleep(time);
		} catch(InterruptedException e) {
			// Restore the interrupted status
			Thread.currentThread().interrupt();
		}
	}
}
