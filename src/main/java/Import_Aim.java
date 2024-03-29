//package ij.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ij.IJ;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
//import ij.process.*;

/** This plugin implements the Acquire/Aim command. */
public class Import_Aim implements PlugIn {

	private int offset;
	private int xdimension;
	private int ydimension;
	private int zdimensions;

	public void run(String arg) {

		OpenDialog od = new OpenDialog("Open Aim...", arg);
		String directory = od.getDirectory();
		String fileName = od.getFileName();
		if (fileName == null)
			return;

		try {
			File iFile = new File(directory + fileName);
			FileInputStream p = new FileInputStream(iFile);

			int offset1 = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);
			int offset2 = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);
			int offset3 = p.read() + p.read() * 256 + p.read() * 65536;

			p.skip(45);

			xdimension = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);
			ydimension = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);
			zdimensions = p.read() + p.read() * 256 + p.read() * 65536;

			offset = offset1 + offset2 + offset3;

			p.skip(61);

			// Find element size:

			int tmpInt = (p.read() << 16) | (p.read() << 24) | p.read() | (p.read() << 8);
			float el_size_mm_x = Float.intBitsToFloat(tmpInt) / 4;
			tmpInt = (p.read() << 16) | (p.read() << 24) | p.read() | (p.read() << 8);
			float el_size_mm_y = Float.intBitsToFloat(tmpInt) / 4;
			tmpInt = (p.read() << 16) | (p.read() << 24) | p.read() | (p.read() << 8);
			float el_size_mm_z = Float.intBitsToFloat(tmpInt) / 4;

			IJ.showMessage("el_size x (in mm): " + el_size_mm_x + "\nel_size y (in mm): " + el_size_mm_y
					+ "\nel_size z (in mm): " + el_size_mm_z);
			// IJ.showMessage("offset1: "+offset1 +"\noffset2: "+offset2 +"\noffset3:
			// "+offset3+"\noffset: "+offset
			// + "\nxdimension: " +xdimension + "\nydimension: " +ydimension +
			// "\nzdimensions: " +zdimensions);

		} catch (IOException e) {
			System.out.println("IOException error!" + e.getMessage());
		}

		// Other version to find parameters ( Michael Gerber, 05-03-2003)

		// Settings for file

		FileInfo fi = new FileInfo();
		fi.fileFormat = FileInfo.RAW;
		fi.fileName = fileName;
		fi.directory = directory;
		fi.width = xdimension;
		fi.height = ydimension;
		fi.offset = offset;
		fi.nImages = zdimensions;
		fi.gapBetweenImages = 0;
		fi.intelByteOrder = true;
		fi.whiteIsZero = true;
		fi.fileType = FileInfo.GRAY16_SIGNED;

		// Open the file
		FileOpener fo = new FileOpener(fi);
		fo.open();

	}

}
