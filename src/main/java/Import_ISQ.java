//package ij.plugin;

//B. Koller, SCANCO Medical AG, April 2005

import ij.plugin.PlugIn;
import java.awt.*;
import java.io.*;
import ij.*;
import ij.io.*;
import java.util.*;
import ij.plugin.frame.*;
import ij.ImageJ;
//import ij.process.*;


/** This plugin implements the Acquire/ISQ command. */
public class Import_ISQ implements PlugIn {

	int record = 0;
        int recCount = 0;
        int i,offset,offset1,offset2,offset3,xdimension,ydimension,zdimension;
	int tmpInt;

	float el_size_mm_x, el_size_mm_y, el_size_mm_z;
	float tmp_float;


	private static String defaultDirectory = null;
	public void run(String arg) {




		OpenDialog od = new OpenDialog("Open ISQ...", arg);
		String directory = od.getDirectory();
		String fileName = od.getFileName();
		if (fileName==null)
			return;

		try  {
			File iFile = new File(directory+fileName);
			FileInputStream p = new FileInputStream(iFile);



			/**
			Ein FileInputStream stellt einen Byte-Stream zum Lesen aus einer Datei zur Verfügung.

			Die read-Methoden dienen dazu, Bytes zu lesen.
			Sie können entweder einzelne Bytes lesen (die als int zurückgegeben werden,
			dessen obere 3 Byte leer sind) oder ihre Daten direkt in einen Bytearray-Puffer schreiben.
			Mit skip kann eine beliebige Anzahl Bytes übersprungen werden.
			**/


			p.skip(44);
			xdimension = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);
			ydimension = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);
			zdimension = p.read() + p.read() * 256 + p.read() * 65536;
                        p.skip(1);
			tmpInt = (p.read() + p.read() * 256 + p.read() * 65536 + p.read() *256*65536);
			el_size_mm_x = tmpInt/xdimension;
			tmpInt = (p.read() + p.read() * 256 + p.read() * 65536 + p.read() *256*65536);
			el_size_mm_y = tmpInt/ydimension;
			tmpInt = (p.read() + p.read() * 256 + p.read() * 65536 + p.read() *256*65536);
			el_size_mm_z = tmpInt/zdimension;
			el_size_mm_x = el_size_mm_x/1000;
			el_size_mm_y = el_size_mm_y/1000;
			el_size_mm_z = el_size_mm_z/1000;

			p.skip(440);

			offset=(p.read() + p.read() * 256 + p.read() * 65536 +1)*512;




		IJ.showMessage("el_size x (in mm): "+ el_size_mm_x+"\nel_size y (in mm): "+ el_size_mm_y+
				"\nel_size z (in mm): "+ el_size_mm_z);
        	IJ.showMessage("offset: "+offset
                       + "\nxdimension: " +xdimension + "\nydimension: " +ydimension + "\nzdimension: " +zdimension);

		}
                 catch (IOException e) {
			System.out.println("IOException error!" + e.getMessage());
		 }




		// Other version to find parameters ( Michael Gerber, 05-03-2003)

		// Settings for file

		FileInfo fi = new FileInfo();
		fi.fileFormat = fi.RAW;
		fi.fileName = fileName;
		fi.directory = directory;
		fi.width = xdimension;
		fi.height = ydimension;
		fi.offset = offset;
		fi.nImages = zdimension;
		fi.gapBetweenImages = 0;
		fi.intelByteOrder = true;
		fi.whiteIsZero = true;
		fi.fileType = FileInfo.GRAY16_SIGNED;



		// Open the file
		FileOpener fo = new FileOpener(fi);
		fo.open();


	}

}






