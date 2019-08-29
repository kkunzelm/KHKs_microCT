import java.awt.*;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

/**
 * Counts the thresholded voxels in a stack and displays the count, average
 * count per slice and the volume fraction.
 * 
 * 2002/01/11: First vesion 2002/01/28: Works with binary stacks 2002/08/19:
 * Volume fraction based on ROI 2006/05/11: Measures volume in real-world units
 * (e.g., mm^3)
 *
 * This plugin Counts the thresholded voxels in a stack and displays the count,
 * the average count per slice and the volume fraction (ratio of thresholded
 * voxels to all voxels). If there is an ROI, the voxel counts and volume
 * fraction are based on the substack defined by that ROI. Before running,
 * threshold the stack using Image/Adjust/Threshold. With binary stacks, black
 * voxels are assummed to be thresholded.
 * 
 * The following summary information is provided:
 * 
 * Thresholded voxels - count of thresolded voxels inside ROI Average voxels per
 * slice - average number of thresolded voxels per slice Total ROI voxels -
 * count of all voxels within ROI Volume fraction - 100*(Thresolded
 * voxels)/(Total ROI voxels) Voxels in stack - number of voxels in stack. With
 * no ROI, this is the same as "Total ROI voxels".
 * 
 * Additional information is provided if the stack is spatially calibrated:
 * 
 * Voxel size - the width, height and depth of a single voxel, as defined in the
 * Image>Properties dialog box Thresholded volume - thresholded voxels * voxel
 * size Average volume per slice - average voxels per slice * voxel size Total
 * ROI volume - total ROI voxels * voxel size Volume of stack - voxels in stack
 * * voxel size
 * 
 * Note that the Analyze>Histogram command also counts thresholded voxels when
 * "Limit to Threshold" is checked in Analyze>Set Measurements.
 * 
 */
public class Voxel_Counter implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.noImage();
			return;
		}
		if (!(imp.getProcessor() instanceof ByteProcessor)) {
			IJ.showMessage("Voxel Counter", "8-bit image or stack required.");
			return;
		}
		if (imp.getProcessor().getMinThreshold() == ImageProcessor.NO_THRESHOLD && !binaryImage(imp)) {
			IJ.showMessage("Voxel Counter", "Thresolded or binary stack required.");
			return;
		}
		Calibration cal = imp.getCalibration();
		double pw = cal.pixelWidth;
		double ph = cal.pixelHeight;
		double pd = cal.pixelDepth;
		cal.pixelWidth = 1.0;
		cal.pixelHeight = 1.0;

		int nslices = imp.getStackSize();
		Roi roi = imp.getRoi();
		int roiCount;
		ResultsTable rt;
		int volumeCount = imp.getWidth() * imp.getHeight() * nslices;
		if (roi == null)
			roiCount = volumeCount;
		else if (roi.getType() == Roi.RECTANGLE) {
			Rectangle r = roi.getBounds();
			roiCount = r.width * r.height * nslices;
		} else {
			IJ.run("Clear Results");
			IJ.run("Set Measurements...", "area decimal=2");
			for (int i = 1; i <= nslices; i++) {
				imp.setSlice(i);
				IJ.run("Measure");
			}
			rt = Analyzer.getResultsTable();
			roiCount = 0;
			for (int i = 0; i < rt.getCounter(); i++)
				roiCount += rt.getValueAsDouble(ResultsTable.AREA, i);
		}

		IJ.run("Clear Results");
		IJ.run("Set Measurements...", "area limit decimal=2");
		for (int i = 1; i <= nslices; i++) {
			imp.setSlice(i);
			IJ.run("Measure");
		}
		cal.pixelWidth = pw;
		cal.pixelHeight = ph;
		rt = Analyzer.getResultsTable();
		double sum = 0;
		for (int i = 0; i < rt.getCounter(); i++)
			sum += rt.getValueAsDouble(ResultsTable.AREA, i);
		IJ.write("");
		IJ.write("Thresholded voxels: " + (int) sum);
		IJ.write("Average voxels per slice:  " + IJ.d2s(sum / nslices, 2));
		IJ.write("Total ROI Voxels: " + roiCount);
		IJ.write("Volume fraction:  " + IJ.d2s((sum * 100) / roiCount, 2) + "%");
		IJ.write("Voxels in stack: " + volumeCount);
		if (cal.scaled()) {
			int digits = Analyzer.getPrecision();
			String units = cal.getUnits();
			double scale = pw * ph * pd;
			IJ.write("");
			IJ.write("Voxel size: " + IJ.d2s(cal.pixelWidth, digits) + "x" + IJ.d2s(cal.pixelHeight, digits) + "x"
					+ IJ.d2s(cal.pixelDepth, digits) + " " + units);
			IJ.write("Thresholded volume: " + IJ.d2s(sum * scale, digits) + " " + units + "^3");
			IJ.write("Average volume per slice:  " + IJ.d2s(sum * scale / nslices, digits) + " " + cal.getUnits()
					+ "^3");
			IJ.write("Total ROI volume: " + IJ.d2s(roiCount * scale, digits) + " " + units + "^3");
			IJ.write("Volume of stack: " + IJ.d2s(volumeCount * scale, digits) + " " + units + "^3");
		}
	}

	private boolean binaryImage(ImagePlus imp) {
		ImageStatistics stats = imp.getStatistics();
		boolean isBinary = stats.histogram[0] + stats.histogram[255] == stats.pixelCount;
		if (isBinary) {
			boolean invertedLut = imp.isInvertedLut();
			ImageProcessor ip = imp.getProcessor();
			if (invertedLut)
				ip.setThreshold(255, 255, ImageProcessor.RED_LUT);
			else
				ip.setThreshold(0, 0, ImageProcessor.RED_LUT);
		}
		return isBinary;
	}

}
