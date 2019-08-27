//package ij.plugin;

import ij.plugin.PlugIn;
import java.awt.*;
import java.io.*;
import ij.*;
import ij.io.*;
import java.util.*;
import ij.plugin.frame.*;
import ij.ImageJ;
//import ij.process.*;


/** This plugin implements the Acquire/Aim command. */
public class Import_Aim_Header implements PlugIn {

	int record = 0;
        int recCount = 0; 
        int i,offset,offset1,offset2,offset3,xdimension,ydimension, zdimensions;  
	int xpos,ypos,zpos,aimtype;
	int tmpInt;
	
	float el_size_mm_x, el_size_mm_y, el_size_mm_z;
	float tmp_float;

	String typestring = null;
	String logstring = null;

        
	private static String defaultDirectory = null;
	public void run(String arg) {


	

		OpenDialog od = new OpenDialog("Open Aim Header...", arg);
		String directory = od.getDirectory();
		String fileName = od.getFileName();
		if (fileName==null)
			return;
		     
		try  {
			File iFile = new File(directory+fileName);
			FileInputStream p = new FileInputStream(iFile);
			
			offset1 = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);
			offset2 = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);
			offset3 = p.read() + p.read() * 256 + p.read() * 65536;	

			p.skip(29);
			aimtype = p.read()+ p.read() * 256 + p.read() * 65536;
			p.skip(1);
			xpos = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);
			ypos = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);
			zpos = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);

			// skip 45 without type and pos
			xdimension = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);
			ydimension = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);
			zdimensions = p.read() + p.read() * 256 + p.read() * 65536;

			offset=offset1+offset2+offset3;

			p.skip(61);

			// Find element size:

			tmpInt = (p.read() << 16) | (p.read() << 24) | p.read() | (p.read() << 8);
			el_size_mm_x = Float.intBitsToFloat(tmpInt)/4;
			tmpInt = (p.read() << 16) | (p.read() << 24) | p.read() | (p.read() << 8);
			el_size_mm_y = Float.intBitsToFloat(tmpInt)/4;
			tmpInt = (p.read() << 16) | (p.read() << 24) | p.read() | (p.read() << 8);
			el_size_mm_z = Float.intBitsToFloat(tmpInt)/4;

			// skip assoc data pointer
			p.skip(4);

			if(aimtype == 131074) 
				typestring = "signed 2 byte (short)";
			if(aimtype == 65537) 
				typestring = "signed 1 byte (char)";
			if(aimtype == 1376257) 
				typestring = "bin compressed";
			
		IJ.showMessage("For aim version 020 (< 2GB and not float aims):\n "
		+"\naim type: "+typestring
		+"\nheader byte offset: "+offset
		+ "\nx dimension: " +xdimension + "\ny dimension: " +ydimension + "\nz dimension: " +zdimensions
		+ "\nx pos: " +xpos + "\ny pos: " +ypos + "\nz pos: " +zpos
		+"\nel_size x (in mm): "+ el_size_mm_x+"\nel_size y (in mm): "+ el_size_mm_y+ "\nel_size z (in mm): "+ el_size_mm_z 
		);


        	// IJ.showMessage("offset1: "+offset1 +"\noffset2: "+offset2 +"\noffset3: "+offset3+"\noffset: "+offset
                //        + "\nxdimension: " +xdimension + "\nydimension: " +ydimension + "\nzdimension: " +zdimensions);
			
		}
                 catch (IOException e) { 
			System.out.println("IOException error!" + e.getMessage());
		 }
			

	

		// Other version to find parameters ( Michael Gerber, 05-03-2003)

		// Settings for file

		// FileInfo fi = new FileInfo();
		// fi.fileFormat = fi.RAW;
		// fi.fileName = fileName;
		// fi.directory = directory;
		// fi.width = 1;
		// fi.height = 1;
		// fi.offset = offset;
		// fi.nImages = 1;
		// fi.gapBetweenImages = 0;
		// fi.intelByteOrder = true;
		// fi.whiteIsZero = true;
		// fi.fileType = FileInfo.GRAY16_SIGNED;
	


		// Open the file		
		// FileOpener fo = new FileOpener(fi);
		// fo.open();

				
	}
	
}










