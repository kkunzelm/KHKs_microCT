//package ij.plugin;

//K.-H. Kunzelmann, LMU-München, Germany, August 2010 for Werth-rek-Files
//B. Koller, SCANCO Medical AG, April 2005 for ISQ-files

import ij.plugin.PlugIn;
import java.io.*;
import ij.*;
import ij.io.*;



/** This plugin implements the Acquire/rek command. */
public class KHKs_simple_Werth_rek_FileReader implements PlugIn {

	int record = 0;
        int recCount = 0;
        int i,offset,offset1,offset2,offset3,xdimension,ydimension,zdimension;
	int tmpInt;

	float el_size_mm_x, el_size_mm_y, el_size_mm_z;
	float tmp_float;



	public void run(String arg) {




		OpenDialog od = new OpenDialog("Open Werth rek...", arg);
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


			xdimension = p.read() + p.read() * 256;
			ydimension = p.read() + p.read() * 256;
                        p.skip(2);
			zdimension = p.read() + p.read() * 256;


/*
			tmpInt = (p.read() + p.read() * 256 + p.read() * 65536 + p.read() *256*65536);
			el_size_mm_x = tmpInt/xdimension;
			tmpInt = (p.read() + p.read() * 256 + p.read() * 65536 + p.read() *256*65536);
			el_size_mm_y = tmpInt/ydimension;
			tmpInt = (p.read() + p.read() * 256 + p.read() * 65536 + p.read() *256*65536);
			el_size_mm_z = tmpInt/zdimension;

			el_size_mm_x = el_size_mm_x/1000;
			el_size_mm_y = el_size_mm_y/1000;
			el_size_mm_z = el_size_mm_z/1000;
*/
			offset=2048;



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
		fi.whiteIsZero = false;
		fi.fileType = FileInfo.GRAY16_UNSIGNED;



		// Open the file
		FileOpener fo = new FileOpener(fi);
		fo.open();


	}

}






