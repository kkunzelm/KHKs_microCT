import ij.plugin.BrowserLauncher;
import ij.plugin.PlugIn;

public class KHKs_Website implements PlugIn {

    public void run(String arg) {

	try { BrowserLauncher.openURL("http://www.dent.med.uni-muenchen.de/~kkunzelm/exponent-0.96.3/index.php?section=31"); }
	catch (Throwable e) { System.out.println("Could not open default internet browser"); }
    }

}
