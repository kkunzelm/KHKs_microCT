import ij.plugin.BrowserLauncher;
import ij.plugin.PlugIn;

public class KHKs_Website implements PlugIn {

	public void run(String arg) {

		try {
			BrowserLauncher
					.openURL("http://www.kunzelmann.de/6_software-1_introduction.html");
		} catch (Throwable e) {
			System.out.println("Could not open default internet browser");
		}
	}

}
