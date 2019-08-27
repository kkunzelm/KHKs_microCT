/**
//Original author: 					B. Koller, SCANCO Medical AG, April 2005
//Modification to consider ROI selection: 		K.-H. Kunzelmann, Operative Dentistry, LMU-München, Ger, April 2006


// History:
// 24.08.10 adapted to read Werth Messtechnik microct files with the extension "rek"
//
// Dieses File ist inzwischen ein ziemlich übler Hack. Eigentlich darf man den Code gar nicht mehr ansehen.
 * Ich müsste das Ganze einfach neu programmieren.
 * Aber wann halt?

 * Essential information from Werth
 *
 * Header Größe: 2048 Byte
 * Byte Order: little Endian (std PC Format)
 * Wortgröße: unsigned 16 Bit Integer (short)
 *
 * typedef struct tagREKHEADER {
	short	width;			// Image Width
	short	height;			// Image Height
	short	bitsPerValue;	// Bits per Value (8,16,32)
	short	numSlices;		// Number of Images
	char	empty1[572];
	float	fRekNorm;		// Unused
	float	pixelSize;		// Pixel Size [µm]
	float	sliceDist;		// Slice Distance [µm]
	char	empty2[1456];
} REKHEADER;
 *
 *
 *
 *
 * // 14.09.09 added information about the name[] field in the header (nameStringInHeader)
// 
// 22.03.08 old and now irrelevant_code_removed
// 23.03.08 KHKs_Scanco_ISQ_FileReader : Downsampling by factor 2 in x, y and z direction by calculating the average of the pixels which are reduced to one new pixel
//          including adjustment of fi.width, fi.height,fi.depth and fi.unit
// 16.02.08 Bugfix for files larger than 2 GB revisited
// 29.06.06 pixeldistance is taken from the orginal ISQ file header and used for metric scaling in ImageJ (fi.info)
// 02.08.06 Bugfix for files larger than 2 GB (adjustment of offset and longOffset so that Skip will be correct)




 * This file is nothing else then copying the modules which are used by imagej to import
 * raw data or files into this one single file.
 * It helped me a lot to understand imagej
 * and it made it easier to devolop this tool
 *
 * The usage should be self-explanatory.
 *
 * In short you select a ROI for the import by entering the upper left and the lower right coordinate
 * of the rectangle.
 *
 * In order to obtain these coordinates you can deside how many slices you want to import and with which slice
 * you start the import.
 *
 *
 *
 * Then you take paper and pencile and note the required coordinates for the second import attempt.
 *
 * Note: this tool was writen because we have only limited memory on our Windows PCs. It was my very first Java program!
 * 
 * Meaning of the checkboxes:
 * "Scale for lin. attenuation coeff.(LAC):"
 * the ISQ files are signed integer (short) raw data (see Scanco Website for more information).
 * To get a meaningful value for the linear attenuation coefficient the short values have to be divided by 4096.
 * This makes it necessary to use float (4byte) instead of 2byte images, which consumes more memory
 * It should be default to use the scaled images for any grey level evaluation, if the morphology is the only important criterion 
 * then scaling is unnecessary and safes memory.
 *
 *  "Downsample by factor 2 in x,y,z (method = average:)"
 * for a view overview on a computer with limited RAM the stack can be downscaled by the factor of 2 in x and y and z direction
 * The downsampling is made by averaging 8 pixels into 1 new pixel 
 *  
 *  "Check = distances in metric units, uncheck = pixels"
 *  Option to decide whether to import the images with metric units as taken from the ISQ-file or alternatively keep the distances in pixels
 *  The pixel variant is useful to determine the ROI, otherwise it is better to use the metric units
 * 
 *  "8-bit-import (overrules 'Scale for LAC')"
 *  To save even more RAM then with Short one can decide to use 8-Bit stacks only. 
 *  The 8-Bit-Stack is a massive data reduction. The values are a simple division of the short values by 256.
 *  For better visability - auto-adjust brightness and contrast yourself.
 *  To get a quick idea of the LAC based on the 8-bit-data you can multiply by 256 and divide by 4096 yourself. But be aware you will lose precision this way due to rounding errors. 
 * 
 * Please consider: this tool was writen by a dentist
 * I have no programming experience.
 * I am satisfied if the code is running.
 * Programming esthetics is nice, but it is beyond my capabilities.
 *
 *
 * TODOs: Select the ROI with the mouse
 **/

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

/** This plugin implements the Acquire/ISQ command. */
public class KHKs_Werth_rek_FileReader implements PlugIn {

	Calibration cal;
	Calibration calOrg;
	int record = 0;
	int recCount = 0;
	int xdimension, ydimension, zdimension;
	long offset;
	int tmpInt;
	boolean scale4096 = false;
	boolean downsample = false;
	boolean metricCalibrationOrPixels = false;
	boolean eightBitOnly = false;
	boolean debug = false;

	// Anpassung für Files > 2 GB
	// wäre aber vermutlich gar nicht nötig. änderung bei Zeile 276 (ca) haette
	// vermutlich gereicht
	float el_size_mm_x, el_size_mm_y, el_size_mm_z;
	float tmp_float;
	String nameStringInHeader = "";
	int startROI, endROI, gapBetweenLines, heightROI, widthROI, nFirstSlice;
	float[] pixels;
	private FileInfo fi;
	private int width, height;
	private long skipCount;
	private int bytesPerPixel, bufferSize, byteCount, nPixels;
	private boolean showProgressBar = true;

	// necessary for the clip ROI
	private int eofErrorCount;
	private String path;
	private int upperLeftX, upperLeftY, lowerRightX, lowerRightY;

	public void run(String arg) {

		// the ISQ-File is selected

		OpenDialog od = new OpenDialog("Open rek...", arg);
		String directory = od.getDirectory();
		String fileName = od.getFileName();
		path = directory + fileName;
		if (fileName == null)
			return;

		// a FileInputStream is opened with the File selected above

		try {
			File iFile = new File(directory + fileName);
			FileInputStream p = new FileInputStream(iFile);

			/**
			 * The header of the rek-File is read
			 *
			 * Ein FileInputStream stellt einen Byte-Stream zum Lesen aus einer Datei zur
			 * Verfügung.
			 * 
			 * Die read-Methoden dienen dazu, Bytes zu lesen. Sie können entweder einzelne
			 * Bytes lesen (die als int zurückgegeben werden, dessen obere 3 Byte leer sind)
			 * oder ihre Daten direkt in einen Bytearray-Puffer schreiben. Mit skip kann
			 * eine beliebige Anzahl Bytes übersprungen werden.
			 **/

			// char: The char data type is a single 16-bit Unicode character. It has a
			// minimum value of '\u0000' (or 0) and a maximum value of '\uffff' (or 65,535
			// inclusive).

			xdimension = p.read() + p.read() * 256;
			ydimension = p.read() + p.read() * 256;
			p.skip(2);
			zdimension = p.read() + p.read() * 256;

			offset = 2048;

			/*
			 * p.skip(440);
			 * 
			 * offset=(p.read() + p.read() * 256 + p.read() * 65536 +1)*512;
			 * System.out.println("offset from header: "+offset);
			 * 
			 */

			p.close();

			/**
			 * IJ.showMessage("el_size x (in mm): "+ el_size_mm_x+"\nel_size y (in mm): "+
			 * el_size_mm_y+ "\nel_size z (in mm): "+ el_size_mm_z); IJ.showMessage("offset:
			 * "+offset + "\nxdimension: " +xdimension + "\nydimension: " +ydimension +
			 * "\nzdimension: " +zdimension);
			 **/

		} catch (IOException e) {

		}

		// Other version to find parameters ( Michael Gerber, 05-03-2003)
		// Settings for file

		// FileInfo
		fi = new FileInfo();

		// fi.fileFormat = fi.RAW;
		fi.fileName = fileName;
		fi.directory = directory;
		fi.width = xdimension;
		fi.height = ydimension;

		// hier Anpassung fuer Files > 2 GB

		if (offset <= 2147483647 && offset > 0) {
			fi.offset = (int) offset;
		}
		if (offset > 2147483647) {
			fi.longOffset = offset;
		}
		if (debug)
			System.out.println("checkpoint1");
		fi.nImages = zdimension;
		fi.gapBetweenImages = 0;
		fi.intelByteOrder = true;
		fi.whiteIsZero = false;
		fi.fileType = FileInfo.GRAY16_UNSIGNED;
		if (debug)
			System.out.println("checkpoint2");
		width = fi.width;
		height = fi.height;

		// Generic dialog to input the ROI-coordinates
		getRoiCoordinates();

		// Open the file

		if (debug)
			System.out.println("checkpoint4");
		openStack_kh();

	}

	/**
	 * *************************************end of "run"
	 * ******************************************
	 **/
	/**
	 * ********************************************************************************************
	 **/

	// Generic dialog to input the ROI-coordinates
	void getRoiCoordinates() {

		GenericDialog gd = new GenericDialog("Kunzelmann: Import Werth rek-Files");

		String text1 = "offset: " + offset + "\nxdimension: " + xdimension + "\nydimension: " + ydimension
                + "\nzdimension: " + zdimension;

		nFirstSlice = 0;

		gd.addMessage(nameStringInHeader + "\n");
		gd.addMessage(text1);

		gd.addMessage("\nEnter the coordinates for the \nbounding rectangle to crop the \nmicroCT stack during import");

		gd.addNumericField("x-coord_ul of the upper left corner: ", upperLeftX, 0);
		gd.addNumericField("y-coord_ul of the upper left corner: ", upperLeftY, 0);
		gd.addNumericField("x-coord_lr of the lower right corner: ", width - 1, 0);
		gd.addNumericField("y_coord_lr of the lower right corner: ", height - 1, 0);
		gd.addNumericField("no. of  z-slices: ", fi.nImages, 0);
		gd.addNumericField("start import at slice no.: ", nFirstSlice, 0);

		gd.addCheckbox("Downsample by factor 2 in x,y,z (method=average): ", downsample);
		// gd.addCheckbox("Check = distances in metric units, uncheck = pixels",
		// metricCalibrationOrPixels);
		// gd.addCheckbox("8-bit-import (overrules 'Scale for LAC')", eightBitOnly);

		gd.showDialog();
		if (gd.wasCanceled()) {
			fi.nImages = 0;
			nFirstSlice = 1;
			return;
		}

		upperLeftX = (int) gd.getNextNumber();
		upperLeftY = (int) gd.getNextNumber();
		lowerRightX = (int) gd.getNextNumber();
		lowerRightY = (int) gd.getNextNumber();
		fi.nImages = (int) gd.getNextNumber();
		nFirstSlice = (int) gd.getNextNumber();

		downsample = gd.getNextBoolean();
		// metricCalibrationOrPixels = gd.getNextBoolean();
		// eightBitOnly = gd.getNextBoolean();
		// if (eightBitOnly == true) scale4096 = false;

		startROI = upperLeftY * width + upperLeftX;

		gapBetweenLines = (width - lowerRightX) + upperLeftX - 1;
		widthROI = lowerRightX - upperLeftX + 1;
		heightROI = lowerRightY - upperLeftY + 1;

		if (nFirstSlice > 0) {
			long dummy;

			long area;
			long sliceTimesArea;
			long sliceTimesAreaTimes2;

			area = fi.width * fi.height;
			sliceTimesArea = area * nFirstSlice;
			sliceTimesAreaTimes2 = sliceTimesArea * 2; // * 2 wegen Short = 2 Byte

			dummy = (long) fi.offset + sliceTimesAreaTimes2;

			if (dummy <= 2147483647 && dummy > 0) {
				fi.offset = fi.offset + (nFirstSlice * fi.width * fi.height * 2); // 2 is hardcoded no. of bytesPerPixel
																					// (short)
			} else {
				fi.longOffset = fi.offset + sliceTimesAreaTimes2;
			}
		}
		System.out.println("offset: " + fi.offset);
		System.out.println("longoffset: " + fi.longOffset);
		if (fi.nImages > zdimension - nFirstSlice) {
			fi.nImages = zdimension - nFirstSlice;
		}

	}

	/** /** Opens a stack of images. */

	void openStack_kh() {
		System.out.println("downsample: " + downsample);

		int widthStack = 0;
		int heightStack = 0;

		if (downsample == true) {

			fi.pixelWidth = fi.pixelWidth * 2;
			fi.pixelHeight = fi.pixelHeight * 2;
			fi.pixelDepth = el_size_mm_z * 2;
			widthStack = widthROI / 2;
			heightStack = heightROI / 2;
		} else {
			widthStack = widthROI;
			heightStack = heightROI;
		}

		// temp stack, needed later for downsampling
		float[] downsampledPixels32_temp = new float[(widthROI * heightROI) / (2 * 2)];

		// modified to match the size of the ROI
		ImageStack stack = new ImageStack(widthStack, heightStack);
		long skip = fi.longOffset > 0 ? fi.longOffset : fi.offset;
		// System.out.println("longoffset-vor-skip: "+fi.longOffset);
		// System.out.println("offset-vor-skip: "+offset);
		// System.out.println("skip: "+skip);
		System.out.println("after Imagestack -> x: " + fi.pixelWidth + " ; y: " + fi.pixelHeight);

		try {
			FileInputStream is = new FileInputStream(path);

			System.out.println("Try FileInputStream");

			if (is == null)
				return;

			for (int i = 1; i <= fi.nImages; i++) { // Obsolet comment: I reduce the no of slices by 1 to avoid a
													// nullpointerexception error
				IJ.showStatus("Reading: " + i + "/" + fi.nImages);
				// System.out.println("fi.nImages: "+fi.nImages);
				pixels = readPixels_kh(is, skip);

				// get pixels for ROI only
				int indexCountPixels = startROI;
				int indexCountROI = 0;

				float[] pixelsROI;
				pixelsROI = new float[widthROI * heightROI];

				for (int u = 0; u < heightROI; u++) {

					System.arraycopy(pixels, indexCountPixels, pixelsROI, indexCountROI, widthROI);

					indexCountPixels = indexCountPixels + widthROI + gapBetweenLines;
					indexCountROI = indexCountROI + widthROI;

					// System.out.println(i+"::"+"indexCountPixels:"+indexCountPixels+":"+"IndexCountROI:"+indexCountROI+":"+"Size:"+widthROI+heightROI);
				}

				if (pixels == null)
					break;

				// **********************dieser Teil ist "short" ***************************

				float[] pixels32 = new float[widthROI * heightROI];
				for (int s = 0; s < widthROI * heightROI; s++) {
					pixels32[s] = (pixelsROI[s]);

				}

				if (downsample == true) {
					// System.out.println("Downsample loop ... ");
					float[] downsampledPixels32 = new float[(widthROI * heightROI) / (2 * 2)];
					// float[] downsampledPixels32_temp = new float[(widthROI*heightROI)/(2*2)];
					float[] downsampledPixels_av = new float[(widthROI * heightROI) / (2 * 2)];

					int index = 0;
					// here we calculate the average in the x,y plane.
					for (int h = 0; h < heightROI - 1; h = h + 2) {
						for (int w = 0; w < widthROI - 1; w = w + 2) {
							downsampledPixels32[index] = ((pixels32[(h * widthROI) + w]
									+ pixels32[(h * widthROI) + w + 1] + pixels32[((h + 1) * widthROI) + w]
									+ pixels32[((h + 1) * widthROI) + w + 1]) / 4);
							index = index + 1;
							if (index >= widthStack * heightStack)
								index = 0;
							// System.out.print(".");
						}
					}
					if (i % 2 > 0) {
						System.arraycopy(downsampledPixels32, 0, downsampledPixels32_temp, 0,
								downsampledPixels32.length);
					} else {
						float temp1, temp2, temp3 = 0;
						for (int s = 0; s < heightStack * widthStack; s++) {
							temp1 = downsampledPixels32[s];
							temp2 = downsampledPixels32_temp[s];
							temp3 = ((temp1 + temp2) / 2);

							downsampledPixels_av[s] = temp3;
						}

						stack.addSlice("microCT-Import_by_KH_w_" + widthStack + "_h_" + heightStack + "_slice." + i,
								downsampledPixels_av);

						// stack.addSlice("microCT-Import_by_KH_w_"+widthROI/2+"_h_"+heightROI/2+"_slice."+
						// i, downsampledPixels_av);
					}
				} else {
					// System.out.println("Instead of downsample loop ... ELSE loop");

					stack.addSlice("microCT-Import_by_KH_w_" + widthROI + "_h_" + heightROI + "_slice." + i, pixelsROI);

				}

				// *************************************************
				// war orginal
				// stack.addSlice("microCT-Import_by_KH_w_"+widthROI+"_h_"+heightROI+"_slice."+
				// i, pixelsROI);

				skip = fi.gapBetweenImages;
				IJ.showProgress((double) i / fi.nImages);
			}
			is.close();
		} catch (Exception e) {
			IJ.log("" + e);
		} catch (OutOfMemoryError e) {
			IJ.outOfMemory(fi.fileName);
			stack.trim();
		}
		IJ.showProgress(1.0);
		if (stack.getSize() == 0)
			return;
		if (fi.sliceLabels != null && fi.sliceLabels.length <= stack.getSize()) {
			for (int i = 0; i < fi.sliceLabels.length; i++)
				stack.setSliceLabel(fi.sliceLabels[i], i + 1);
		}
		ImagePlus imp = new ImagePlus(fi.fileName, stack);
		cal = imp.getCalibration();
		calOrg = cal.copy();

		if (fi.info != null)
			imp.setProperty("Info", fi.info);
		imp.show();
		imp.setFileInfo(fi);
		System.out.println("after imp.show() -> x: " + fi.pixelWidth + " ; y: " + fi.pixelHeight);

		/*
		 * cal.pixelWidth = el_size_mm_x; cal.pixelHeight = el_size_mm_y; cal.pixelDepth
		 * = el_size_mm_z; cal.setUnit("mm");
		 */

		// if (metricCalibrationOrPixels==true) imp.setCalibration(cal);

		if (metricCalibrationOrPixels == true) {
			cal.pixelWidth = el_size_mm_x;
			cal.pixelHeight = el_size_mm_y;
			cal.pixelDepth = el_size_mm_z;
			cal.setUnit("mm");
			imp.setCalibration(cal);
		} else {
			cal.pixelWidth = 1.0D;
			cal.pixelHeight = 1.0D;
			cal.pixelDepth = 1.0D;
			cal.setUnit("pixel");
			imp.setCalibration(cal);
		}

		ImageProcessor ip = imp.getProcessor();
		if (ip.getMin() == ip.getMax()) // find stack min and max if first slice is blank
			setStackDisplayRange_kh(imp);
		IJ.showProgress(1.0);
		return;
	}

	void setStackDisplayRange_kh(ImagePlus imp) {
		ImageStack stack = imp.getStack();
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		int n = stack.getSize();
		for (int i = 1; i <= n; i++) {
			IJ.showStatus("Calculating stack min and max: " + i + "/" + n);
			ImageProcessor ip = stack.getProcessor(i);
			ip.resetMinAndMax();
			if (ip.getMin() < min)
				min = ip.getMin();
			if (ip.getMax() > max)
				max = ip.getMax();
		}
		imp.getProcessor().setMinAndMax(min, max);
		imp.updateAndDraw();
	}

	/** *********************************************************************** **/
	/**
	 * aus ImageReader.java: Skips the specified number of bytes, then reads an
	 * image and returns the pixel array (byte, short, int or float). Returns null
	 * if there was an IO exception. Does not close the InputStream.
	 */
	public float[] readPixels_kh(FileInputStream in, long skipCount) {
		this.skipCount = skipCount;
		showProgressBar = false;
		pixels = readPixels_kh(in);
		if (eofErrorCount > 0)
			return null;
		else
			return pixels;
	}

	/**
	 * Reads the image from the InputStream and returns the pixel array (byte,
	 * short, int or float). Returns null if there was an IO exception. Does not
	 * close the InputStream.
	 */
	public float[] readPixels_kh(FileInputStream in) {
		try {

			bytesPerPixel = 2;
			skip_kh(in);
			return read16bitImage_kh(in);

		} catch (IOException e) {
			IJ.log("" + e);
			return null;
		}
	}

	/**
	 * *********************** this is the central import routine
	 * ***********************************
	 **/
	// there is still room to reduce code bits which are not really necessary for
	// the ISQ-Import.

	/**
	 * Reads a 16-bit image. Signed pixels are converted to unsigned by adding
	 * 32768.
	 */
	float[] read16bitImage_kh(FileInputStream in) throws IOException {
		int pixelsRead;
		byte[] buffer = new byte[bufferSize];
		pixels = new float[nPixels];
		int totalRead = 0;
		int base = 0;
		int count;
		int bufferCount;

		while (totalRead < byteCount) {
			if ((totalRead + bufferSize) > byteCount)
				bufferSize = byteCount - totalRead;
			bufferCount = 0;

			while (bufferCount < bufferSize) { // fill the buffer
				count = in.read(buffer, bufferCount, bufferSize - bufferCount);
				if (count == -1) {
					eofError();

				}
				bufferCount += count;
			}
			totalRead += bufferSize;
			showProgress((double) totalRead / byteCount);
			pixelsRead = bufferSize / bytesPerPixel;

			for (int i = base, j = 0; i < (base + pixelsRead); i++, j += 2)
				pixels[i] = (float) (((buffer[j + 1] & 0xff) << 8) | (buffer[j] & 0xff));

			base += pixelsRead;
		}
		return pixels;
	}

	void skip_kh(FileInputStream in) throws IOException {

		// I count, how often this routine is used:
		// System.out.println("skip_kh called");
		// answer: called for every slice

		if (skipCount > 0) {
			long bytesRead = 0;
			int skipAttempts = 0;
			long count;
			while (bytesRead < skipCount) {
				count = in.skip(skipCount - bytesRead);
				skipAttempts++;
				if (count == -1 || skipAttempts > 5)
					break;
				bytesRead += count;
				// IJ.log("skip: "+skipCount+" "+count+" "+bytesRead+" "+skipAttempts);
			}
		}
		byteCount = width * height * bytesPerPixel;

		nPixels = width * height;
		bufferSize = byteCount / 25;
		if (bufferSize < 8192)
			bufferSize = 8192;
		else
			bufferSize = (bufferSize / 8192) * 8192;
	}

	void eofError() {
		eofErrorCount++;
	}

	private void showProgress(double progress) {
		if (showProgressBar)
			IJ.showProgress(progress);
	}

	/*
	 * void
	 */

}
