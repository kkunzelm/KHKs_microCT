import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.FileSaver;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;

/**
 * usage KH: to omitt the raw extension and to get numbered raw files 
 * checkmark the option use labels for the filename
 **/

/** Writes the slices of stack as separate files. */
public class StackWriter_addedSlicer implements PlugIn {

	// private static String defaultDirectory = null;

	private static final String[] choices = {"SlicerRaw", "Tiff", "Gif", "Jpeg", "Bmp", "Raw", "Zip", "Text"};
	private static String fileType = "SlicerRaw";
	private static int ndigits = 3;
	private static int startAt;
	private static boolean useLabels = true;
	// private static boolean startAtZero;

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null || imp.getStackSize() < 2) {
			IJ.error("Stack Writer", "This command requires a stack.");
			return;
		}
		int stackSize = imp.getStackSize();
		String name = imp.getTitle();

		int dotIndex = name.lastIndexOf(".");
		if (dotIndex >= 0)
			name = name.substring(0, dotIndex);

		GenericDialog gd = new GenericDialog("Save Image Sequence");
		gd.addChoice("Format:", choices, fileType);
		gd.addStringField("Name:", name, 12);
		gd.addNumericField("Start At:", startAt, 0);
		gd.addNumericField("Digits (1-8):", ndigits, 0);
		gd.addCheckbox("Use Slice Labels as File Names", useLabels);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		fileType = gd.getNextChoice();
		name = gd.getNextString();
		startAt = (int) gd.getNextNumber();
		if (startAt < 0)
			startAt = 0;
		ndigits = (int) gd.getNextNumber();
		useLabels = gd.getNextBoolean();
		int number = 0;
		if (ndigits < 1)
			ndigits = 1;
		if (ndigits > 8)
			ndigits = 8;
		int maxImages = (int) Math.pow(10, ndigits);
		if (stackSize > maxImages && !useLabels) {
			IJ.error("Stack Writer", "More than " + ndigits
					+ " digits are required to generate \nunique file names for " + stackSize + " images.");
			return;
		}
		if (fileType.equals("Gif") && !FileSaver.okForGif(imp))
			return;

		String extension = "";
		switch (fileType) {
			case "SclicerRaw":
				extension = "";
				break;
			case "Tiff":
				extension = ".tif";
				break;
			case "Jpeg":
				extension = ".jpg";
				break;
			case "Gif":
				extension = ".gif";
				break;
			case "Bmp":
				extension = ".bmp";
				break;
			case "Raw":
				extension = ".raw";
				break;
			case "Zip":
				extension = ".zip";
				break;
			case "Text":
				extension = ".txt";
				break;
		}

		String digits = getDigits(number);

		SaveDialog sd = new SaveDialog("Save Image Sequence", name + digits + extension, extension);

		String name2 = sd.getFileName();
		if (name2 == null)
			return;
		String directory = sd.getDirectory();

		ImageStack stack = imp.getStack();
		ImagePlus tmp = new ImagePlus();
		tmp.setTitle(imp.getTitle());
		int nSlices = stack.getSize();
		String path, path2, label = null;

		label:
		for (int i = 1; i <= nSlices; i++) {
			IJ.showStatus("writing: " + i + "/" + nSlices);
			IJ.showProgress((double) i / nSlices);
			tmp.setProcessor(null, stack.getProcessor(i));
			digits = getDigits(number++);
			if (useLabels) {
				label = stack.getShortSliceLabel(i);
				if (label != null && label.equals(""))
					label = null;
				if (label != null) {

					int index = label.lastIndexOf(".");
					if (index >= 0)
						label = label.substring(0, index);
				}
			}

			if (label == null)
				path = directory + name + digits + extension;
			else
				path = directory + label + extension;

			path2 = directory + label + "." + digits;

			switch (fileType) {
				case "Tiff":
					if (!(new FileSaver(tmp).saveAsTiff(path)))
						break label;
					break;
				case "Gif":
					if (!(new FileSaver(tmp).saveAsGif(path)))
						break label;
					break;
				case "Jpeg":
					if (!(new FileSaver(tmp).saveAsJpeg(path)))
						break label;
					break;
				case "Bmp":
					if (!(new FileSaver(tmp).saveAsBmp(path)))
						break label;
					break;
				case "Raw":
					if (!(new FileSaver(tmp).saveAsRaw(path)))
						break label;
					break;
				case "SlicerRaw":  // KH
					if (!(new FileSaver(tmp).saveAsRaw(path2))) // KH
						break label;
					break;
				case "Zip":
					tmp.setTitle(name + digits + extension);
					if (!(new FileSaver(tmp).saveAsZip(path)))
						break label;
					break;
				case "Text":
					if (!(new FileSaver(tmp).saveAsText(path)))
						break label;
					break;
			}
			// System.gc();
		}
		IJ.showStatus("");
		IJ.showProgress(1.0);
		IJ.register(StackWriter_addedSlicer.class);
	}

	private String getDigits(int n) {
		String digits = "00000000" + (startAt + n);
		return digits.substring(digits.length() - ndigits);
	}

}
