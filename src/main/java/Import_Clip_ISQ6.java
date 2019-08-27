/**
 

//Original author: 					B. Koller, SCANCO Medical AG, April 2005
//Modification to consider ROI selection: 		K.-H. Kunzelmann, Operative Dentistry, LMU-München, Ger, April 2006


// History:
// 14.09.09 added information about the name[] field in the header (nameStringInHeader)
// 21.08.09 Kopierschutz ergänzt
// 22.3.08 Import_Clip_ISQ5_irrelevant_code_removed is a modified Import_Clip_ISQ4: reduced some ImageJ specific, but unnecessary items  
// 23.3.08 Import_Clip_ISQ6 : Downsampling by factor 2 in x, y and z direction by calculating the average of the pixels which are reduced to one new pixel
//          including adjustment of fi.width, fi.height,fi.depth and fi.unit
// 29.6.06: pixeldistance is taken from the orginal ISQ file header and used for metric scaling in ImageJ (fi.info)
// 02.08.06 Bugfix for files larger than 2 GB (adjustment of offset and longOffset so that Skip will be correct)
// 16.2.08  Bugfix for files larger than 2 GB revisited



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
 * Note: this tool was writen because we have only limited memory on our Windows PCs.
 * According to ImageJ - even with enough RAM - ImageJ is limited to 1.7 GB memory handling capabilities.
 *
 * Meaning of the checkboxes:
 * "Scale for lin. attenuation coeff. (4096):" 
 * the ISQ files are signed integer (short) raw data. 
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
 * todos:
 * Fehler Finden mit NullPointer bei letztem Bild
 *
 * In ferner Zukunft: Auswahl per Rechteck-ROI
 **/

import ij.plugin.PlugIn;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import java.io.*;


/** This plugin implements the Acquire/ISQ command. */
public class Import_Clip_ISQ6 implements PlugIn {

    private FileInfo fi;
    private int width, height;
    private long skipCount;
    private int bytesPerPixel, bufferSize, byteCount, nPixels;
    private boolean showProgressBar=true;
    private int eofErrorCount;
    private String path;
    Calibration cal; 
    Calibration calOrg;

    int record = 0;
    int recCount = 0;
    int xdimension,ydimension,zdimension,mu_scaling;
        
        // Anpassung für Files > 2 GB
        // wäre aber vermutlich gar nicht nötig. änderung bei Zeile 276 (ca) haette vermutlich gereicht
        
    long offset;
	int tmpInt;

    boolean scale4096 = false;
    boolean downsample = false;
    boolean metricCalibrationOrPixels = false;
    boolean eightBitOnly = false;
    boolean debug = false;



    float el_size_mm_x, el_size_mm_y, el_size_mm_z;
    float tmp_float;
    String nameStringInHeader = "";

        // necessary for the clip ROI

    private int upperLeftX, upperLeftY, lowerRightX, lowerRightY;
    int startROI, endROI, gapBetweenLines,heightROI,widthROI,nFirstSlice;
    short[] pixels;


	public void run(String arg) {

        //*********************Begin KHKs Copy Protection *********************
/*
        if (!DateAndMacAddrCheck.mainOfKHKsCopyProctection()){
            System.out.println("Source code line: 127 - Message: I am so sorry...");
            return; 
        }
        else {
           System.out.println("Source code line: 131 - Message: Heureka ...");
        }
 */
 //***********************End KHKs Copy Protection **********************

        // the ISQ-File is selected

		OpenDialog od = new OpenDialog("Open ISQ...", arg);
		String directory = od.getDirectory();
		String fileName = od.getFileName();
                path = directory + fileName;
		if (fileName==null)
			return;

                // a FileInputStream is opened with the File selected above

		try  {
			File iFile = new File(directory+fileName);
			FileInputStream p = new FileInputStream(iFile);



			/**
                         * The header of the ISQ-File is read
                         *
			Ein FileInputStream stellt einen Byte-Stream zum Lesen aus einer Datei zur Verfügung.

			Die read-Methoden dienen dazu, Bytes zu lesen.
			Sie können entweder einzelne Bytes lesen (die als int zurückgegeben werden,
			dessen obere 3 Byte leer sind) oder ihre Daten direkt in einen Bytearray-Puffer schreiben.
			Mit skip kann eine beliebige Anzahl Bytes übersprungen werden.
			**/

                        // char: The char data type is a single 16-bit Unicode character. It has a minimum value of '\u0000' (or 0) and a maximum value of '\uffff' (or 65,535 inclusive).


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

                          p.skip(20);
                        //   82   int     mu_scaling;
                        tmpInt = (p.read() + p.read() * 256 + p.read() * 65536 + p.read() *256*65536);
                        mu_scaling = tmpInt;

                        p.skip(36); // war 60, wegen mu_scaling jetzt 36

                        
                        for (int kh=0; kh < 40; kh++){
                            char ch = (char)p.read();
                            nameStringInHeader = nameStringInHeader + ch;
                            // System.out.println(nameStringInHeader);
                        }

                        
			p.skip(340);

			offset=(p.read() + p.read() * 256 + p.read() * 65536 +1)*512;
                        // System.out.println("offset from header: "+offset);


                        /*
			p.skip(440);

			offset=(p.read() + p.read() * 256 + p.read() * 65536 +1)*512;
                        System.out.println("offset from header: "+offset);

                        */

                        p.close();

        /**
		IJ.showMessage("el_size x (in mm): "+ el_size_mm_x+"\nel_size y (in mm): "+ el_size_mm_y+
		"\nel_size z (in mm): "+ el_size_mm_z);
		IJ.showMessage("offset: "+offset
                       + "\nxdimension: " +xdimension + "\nydimension: " +ydimension + "\nzdimension: " +zdimension);
		**/

			}
                 catch (IOException e) {

			}


  

		// Other version to find parameters ( Michael Gerber, 05-03-2003)

		// Settings for file

		// FileInfo
                fi = new FileInfo();

		//fi.fileFormat = fi.RAW;
		fi.fileName = fileName;
		fi.directory = directory;
		fi.width = xdimension;
		fi.height = ydimension;
                        
                // hier Anpassung fuer Files > 2 GB
 
	
                if (offset<=2147483647 && offset>0){
                    fi.offset = (int)offset;
                }
                if (offset>2147483647){
                    fi.longOffset = offset;
                }
                if (debug) System.out.println ("checkpoint1");
		fi.nImages = zdimension;
		fi.gapBetweenImages = 0;
		fi.intelByteOrder = true;
		fi.whiteIsZero = false;
		fi.fileType = FileInfo.GRAY16_SIGNED;
                if (debug) System.out.println ("checkpoint2");
                width = fi.width;
		height = fi.height;

                // added: 30.6.06 - Measurement in metric units possible
                //System.out.println("vor cal");
                
                
                if (debug)  System.out.println ("checkpoint3");
                
                fi.pixelWidth = (double)el_size_mm_x;
                if (debug)  System.out.println ("checkpoint3a");
                
                fi.pixelHeight = (double)el_size_mm_y;
                fi.pixelDepth = (double)el_size_mm_z;
                
                if (debug) System.out.println ("checkpoint3a");
                
                fi.unit="mm";
                

                System.out.println("Source code line: 252 - Message: Pixel width x: "+fi.pixelWidth +" ; y: "+fi.pixelHeight);
                
                // End added: 23.4.08 - habe her fi. durch cal. ersetzt. Diese Routine überschreibt die fi. Routine.
                // und auch unten bei der set.calibrate oder so ähnlich eingetragen
                
                // Generic dialog to input the ROI-coordinates
                getRoiCoordinates();

		// Open the file

		if (debug)  System.out.println ("checkpoint4");
                openStack_kh();



	}

/** *************************************end of "run" ****************************************** **/
/** ******************************************************************************************** **/

        // Generic dialog to input the ROI-coordinates
        void getRoiCoordinates(){

                        GenericDialog gd = new GenericDialog("Kunzelmann: Import Scanco ISQ-Files");

    

                        String text1=new String ("offset: "+offset+ "\nxdimension: " +xdimension + "\nydimension: " +ydimension + "\nzdimension: " +zdimension);
                        String text2=new String ("el_size x (in mm): "+ el_size_mm_x+"\nel_size y (in mm): "+ el_size_mm_y+
				"\nel_size z (in mm): "+ el_size_mm_z);
                        nFirstSlice = 0;

                        gd.addMessage(nameStringInHeader+"\n");
                        gd.addMessage(text1);
                        gd.addMessage(text2);
                        
                        gd.addMessage("\nmu_scaling: "+mu_scaling+"\n");
                        gd.addMessage("\nEnter the coordinates for the \nbounding rectangle to crop the \nmicroCT stack during import");

                        gd.addNumericField("x-coord_ul of the upper left corner: ", upperLeftX, 0);
                        gd.addNumericField("y-coord_ul of the upper left corner: ", upperLeftY, 0);
                        gd.addNumericField("x-coord_lr of the lower right corner: ", width-1, 0);
                        gd.addNumericField("y_coord_lr of the lower right corner: ", height-1, 0);
                        gd.addNumericField("no. of  z-slices: ", fi.nImages, 0);
                        gd.addNumericField("start import at slice no.: ", nFirstSlice, 0);
                        gd.addCheckbox("Scale for lin. attenuation coeff. (4096): ", scale4096);
                        gd.addCheckbox("Downsample by factor 2 in x,y,z (method=average): ", downsample);
                        gd.addCheckbox("Check = distances in metric units, uncheck = pixels", metricCalibrationOrPixels);
                        gd.addCheckbox("8-bit-import (overrules 'Scale for LAC')", eightBitOnly);
                        
                        
                        gd.showDialog();
                        if (gd.wasCanceled()) {
                            fi.nImages=0;
                            nFirstSlice=1;
                            return;
                        }

                        upperLeftX = (int)gd.getNextNumber();
                        upperLeftY = (int)gd.getNextNumber();
                        lowerRightX = (int)gd.getNextNumber();
                        lowerRightY = (int)gd.getNextNumber();
                        fi.nImages = (int)gd.getNextNumber();
                        nFirstSlice = (int)gd.getNextNumber();
                        scale4096 = gd.getNextBoolean();
                        downsample = gd.getNextBoolean();
                        metricCalibrationOrPixels = gd.getNextBoolean();
                        eightBitOnly = gd.getNextBoolean();
                        if (eightBitOnly == true) scale4096 = false;

                        startROI = upperLeftY*width+upperLeftX;

                        gapBetweenLines = (width-lowerRightX)+upperLeftX-1;
                        widthROI = lowerRightX-upperLeftX+1;
                        heightROI =lowerRightY-upperLeftY+1;

                        //System.out.println("widthROI:heightROI"+widthROI+":"+heightROI);
                        //System.out.println("GapBetweenLines: "+gapBetweenLines);
                        //System.out.println("StartROI: "+startROI);

                        // ***********************************************
                        // Anpassung wegen Files > 2 GB
                        // ich habe aus offset ein long statt integer gemacht
                        // ich habe Abfrage ergänzt of fi.offset oder fi.longOffset verwendet werden soll
                        // ich habe hier änderung ergänzt mit Abfrage zur Grösse
                        
                        if (nFirstSlice > 0){
                            long dummy;
                            
                            long area;
                            long sliceTimesArea;
                            long sliceTimesAreaTimes2;
                            
                            
                            
                            
                            area = fi.width*fi.height;
                            sliceTimesArea = area*nFirstSlice;
                            sliceTimesAreaTimes2 = sliceTimesArea * 2; // * 2 wegen Short = 2 Byte
                            
                            dummy = (long)fi.offset + sliceTimesAreaTimes2;
//                          UrsprÃ¼nglich hatte ich die folgende Zeile verwendet
//                          Damit war aber dummy immer negativ
//                          vermutlich wegen der Multipliation (wird grÃ¶sser als int Wert
//                          Ich zerlege die Multiplikation jetzt und definiere alle Werte als long;
//                          dummy = (long)fi.offset  + (long)(nFirstSlice*fi.width*fi.height*2);
                            

                            /*
                            System.out.println("fi.offset "+fi.offset);
                            System.out.println("nFirstSlice "+nFirstSlice);
                            System.out.println("fi.width "+fi.width);
                            System.out.println("fi.height "+fi.height);
                            System.out.println("fi.offset "+fi.offset);
                            System.out.println("dummy..."+dummy);
                            */

                            //bei einem "int"-Overflow - also Zahl die grÃ¶ÃŸer als Int ist das Ergebnis negativ.
                            // das muss ich auch abfragen.                            
                            // war Stand for 16.2.08: if (fi.offset  + (nFirstSlice*fi.width*fi.height*2) <=2147483647){

                            if (dummy <=2147483647 && dummy>0){
                            fi.offset = fi.offset + (nFirstSlice*fi.width*fi.height*2); // 2 is hardcoded no. of bytesPerPixel (short)
                            }
                            else {
                                fi.longOffset = (long)(fi.offset + sliceTimesAreaTimes2);
                            }
                        }
                        System.out.println("offset: "+fi.offset);
                        System.out.println("longoffset: "+fi.longOffset);
                         if (fi.nImages > zdimension - nFirstSlice){
                            fi.nImages = zdimension - nFirstSlice;                         }

                }

         /** Opens a stack of images. */

	void openStack_kh() {
                System.out.println("downsample: "+downsample);
                
                int widthStack = 0;
                int heightStack = 0;
                
                if (downsample == true){
                    
                    
                    fi.pixelWidth = fi.pixelWidth*2;
                    fi.pixelHeight = fi.pixelHeight*2;
                    fi.pixelDepth = el_size_mm_z*2;
                    widthStack = widthROI/2;
                    heightStack = heightROI/2;                    
                }   
                else {
                widthStack = widthROI;
                heightStack = heightROI;
                }

                // temp stack, needed later for downsampling        
                float[] downsampledPixels32_temp = new float[(widthROI*heightROI)/(2*2)];
               
                // modified to match the size of the ROI
		ImageStack stack = new ImageStack(widthStack, heightStack);
		long skip = fi.longOffset>0?fi.longOffset:fi.offset;
                //System.out.println("longoffset-vor-skip: "+fi.longOffset);
                //System.out.println("offset-vor-skip: "+offset);                
                //System.out.println("skip: "+skip);
		 System.out.println("after Imagestack -> x: "+ fi.pixelWidth+" ; y: "+ fi.pixelHeight);
                 
		try {
			FileInputStream is = new FileInputStream(path);
                        
			System.out.println("Try FileInputStream");
                        
                        if (is==null)
				return ;
                      
                        for (int i=1; i<= fi.nImages; i++) {              //Obsolet comment: I reduce the no of slices by 1 to avoid a nullpointerexception error
				IJ.showStatus("Reading: " + i + "/" + fi.nImages);
                                //System.out.println("fi.nImages: "+fi.nImages);
				pixels = readPixels_kh(is, skip);

                                // get pixels for ROI only
                                int indexCountPixels = startROI;
                                int indexCountROI = 0;

                                short[] pixelsROI;
                                pixelsROI = new short[widthROI*heightROI];

                                for (int u = 0; u < heightROI; u++){


                                     System.arraycopy(pixels, indexCountPixels, pixelsROI, indexCountROI, widthROI);




                                     indexCountPixels = indexCountPixels + widthROI + gapBetweenLines;
                                     indexCountROI    = indexCountROI + widthROI;

                                    // System.out.println(i+"::"+"indexCountPixels:"+indexCountPixels+":"+"IndexCountROI:"+indexCountROI+":"+"Size:"+widthROI+heightROI);
                                    }


                                if (pixels==null) break;
                                
                        

                                if (scale4096==true){
                                        float[] pixels32 = new float[widthROI*heightROI];
                                        for (int s=0; s<widthROI*heightROI; s++){
                                            pixels32[s] = (pixelsROI[s]&0xffff);
                                            // mein Problem ist, dass beim Einlesen mit der Original-Version von Hr. Koller
                                            // die ImageJ Funktionen vollständig genutzt werden.
                                            // so wie ich das im Moment umgesetzt habe, habe ich die Signed-16Bit-Werte in
                                            // unsigned convertiert (durch die Addition von 32768.
                                            // wenn ich nun aber genau so "skalieren" will, wie mit der "Koller-Import-Routine"
                                            // habe ich das Problem, dass die Werte nur positiv sind und um die 32768 zu hoch sind
                                            // ich behebe das hier, indem ich beim Umwandeln in Float das Ganze rückgängig mache
                                            // langfristig, muss ich mal die ganze Routine von Grund auf selbst und neu schreiben.
                                            // aber wann? .. nebenbei: im Fileopener.java wird das ganze genauso gemacht.
                                            
                                            pixels32[s] = pixels32[s]-32768;
                                            // hier wird nun ISQ bzgl. des lin. att. Coeff. skaliert (nächste Zeile)
                                            pixels32[s] = pixels32[s]/4096;
                                            if (pixels32[s]<0){
                                                pixels32[s]=0;
                                            }
                                       }
                                       
                                       if (downsample==true){
                                           System.out.println("Downsample loop ... ");
                                           float[] downsampledPixels32 = new float[(widthROI*heightROI)/(2*2)];
                                           //float[] downsampledPixels32_temp = new float[(widthROI*heightROI)/(2*2)];
                                           float[] downsampledPixels32_av = new float[(widthROI*heightROI)/(2*2)];
                                           int index = 0;
                                           // here we calculate the average in the x,y plane. a
                                           for (int h=0; h < heightROI-1; h=h+2){
                                               for (int w=0; w < widthROI-1; w=w+2){
                                                    downsampledPixels32[index]=((pixels32[(h*widthROI)+w]+ pixels32[(h*widthROI)+w+1] + 
                                                                                 pixels32[((h+1)*widthROI)+w] + pixels32[((h+1)*widthROI)+w+1])/4);
                                                    //System.out.println("Pixel32: "+pixels32[(h*widthROI)+w]+ pixels32[(h*widthROI)+w+1] + pixels32[((h+1)*widthROI)+w] + pixels32[((h+1)*widthROI)+w+1]);
                                                    index = index + 1;
                                                    if (index >= widthStack * heightStack) index = 0;
                                                    System.out.print(".");
                                               }
                                            }
                                            if (i%2>0){
                                                    System.out.println("i%2 erreicht"+i);
                                                    System.arraycopy(downsampledPixels32,0,downsampledPixels32_temp,0,downsampledPixels32.length);
                                                    
                                            }
                                            else {
                                                System.out.println("Else Teil von i%2 erreicht"+i);
                                                float temp1,temp2,temp3 = 0;
                                                for (int s= 0; s < heightStack*widthStack; s++){
                                                    temp1 = downsampledPixels32[s];
                                                    temp2 = downsampledPixels32_temp[s];
                                                    temp3 = (temp1 + temp2)/2;
                                                    //System.out.println("temp1-temp2: "+temp1+"temp2  " +temp2+ "stack:  "+ downsampledPixels32_temp[s] + "temp3  "+ temp3);
                                                    downsampledPixels32_av[s]=temp3;
                                                }
                                                 
                                               
                                                stack.addSlice("microCT-Import_by_KH_w_"+widthROI/2+"_h_"+heightROI/2+"_slice."+ i, downsampledPixels32_av);
                                            }
                                       }
                                       else{ 
                                            // System.out.println("Instead of downsample loop ... ELSE loop");
                                            stack.addSlice("microCT-Import_by_KH_w_"+widthROI+"_h_"+heightROI+"_slice."+ i, pixels32);
                                        }
                                }
                                else {
                                //**********************dieser Teil ist "short" ***************************
                                
                                        float[] pixels32 = new float[widthROI*heightROI];
                                        for (int s=0; s<widthROI*heightROI; s++){
                                            pixels32[s] = (pixelsROI[s]&0xffff);
                                                                                      
                                            pixels32[s] = pixels32[s]-32768;
                                            // hier wird nun ISQ bzgl. des lin. att. Coeff. skaliert (nächste Zeile)
                                            pixels32[s] = pixels32[s]/4096;
                                            if (pixels32[s]<0){
                                                pixels32[s]=0;
                                            }
                                       }
                                       
                                       if (downsample==true){
                                           // System.out.println("Downsample loop ... ");
                                           float[] downsampledPixels32 = new float[(widthROI*heightROI)/(2*2)];
                                           // float[] downsampledPixels32_temp = new float[(widthROI*heightROI)/(2*2)];
                                           short[] downsampledPixels_av = new short[(widthROI*heightROI)/(2*2)];

                                           int index = 0;
                                           // here we calculate the average in the x,y plane. 
                                           for (int h=0; h < heightROI-1; h=h+2){
                                               for (int w=0; w < widthROI-1; w=w+2){
                                                    downsampledPixels32[index]=((pixels32[(h*widthROI)+w]+ pixels32[(h*widthROI)+w+1] + 
                                                                                 pixels32[((h+1)*widthROI)+w] + pixels32[((h+1)*widthROI)+w+1])/4);
                                                    index = index + 1;
                                                    if (index >= widthStack * heightStack) index = 0;
                                                    //System.out.print(".");
                                               }
                                            }
                                            if (i%2>0){
                                                    System.arraycopy(downsampledPixels32,0,downsampledPixels32_temp,0,downsampledPixels32.length);
                                            }
                                            else {
                                                float temp1,temp2,temp3 = 0;
                                                for (int s= 0; s < heightStack*widthStack; s++){
                                                    temp1 = downsampledPixels32[s];
                                                    temp2 = downsampledPixels32_temp[s];
                                                    temp3 = ((temp1 + temp2)/2)*4096;
                                                    if (temp3<0.0) temp3 = 0.0f;
                                                    if (temp3>65535.0) temp3 = 65535.0f;
                                                    downsampledPixels_av[s] = (short)temp3;
                                                }
                                                
                                                //*** shortTo8bit
                                                
                                                int value;
                                                byte[] pixels8 = new byte[heightStack*widthStack];
                                                for (int index2=0; index2<heightStack*widthStack; index2++) {
                                                    value = downsampledPixels_av[index2]/256;
                                                    if (value>255) value = 255;
                                                    pixels8[index2] = (byte)value;
                                               }
                                               
                                                
                                                if (eightBitOnly==true){
                                                    stack.addSlice("microCT-Import_by_KH_w_"+widthStack+"_h_"+heightStack+"_slice."+ i, pixels8); 
                                                }
                                                else {    
                                                    stack.addSlice("microCT-Import_by_KH_w_"+widthStack+"_h_"+heightStack+"_slice."+ i, downsampledPixels_av);
                                                } 
                                                
                                            //*** shortTo8bit ende  
                                            
                                                
                                                
                                               // stack.addSlice("microCT-Import_by_KH_w_"+widthROI/2+"_h_"+heightROI/2+"_slice."+ i, downsampledPixels_av);
                                            }
                                       }
                                       else{ 
                                            //System.out.println("Instead of downsample loop ... ELSE loop");
                                            for (int index = 0; index < widthROI*heightROI; index++){
                                                pixelsROI[index]=(short)(pixelsROI[index]-32768);
                                                if (pixelsROI[index]<0) pixelsROI[index]=0;
                                             }
                                            //*** shortTo8bit
                                                
                                                int value;
                                                byte[] pixels8 = new byte[widthROI*heightROI];
                                                for (int index2=0; index2<widthROI*heightROI; index2++) {
                                                    value = pixelsROI[index2]/256;
                                                    if (value>255) value = 255;
                                                    pixels8[index2] = (byte)value;
                                               }
                                               
                                            //*** shortTo8bit ende 
                                            // System.out.println("Instead of downsample loop ... ELSE loop - just before add stack");
                                            
                                            if (eightBitOnly==true){
                                                stack.addSlice("microCT-Import_by_KH_w_"+widthROI+"_h_"+heightROI+"_slice."+ i, pixels8); 
                                            }
                                            else {    
                                                stack.addSlice("microCT-Import_by_KH_w_"+widthROI+"_h_"+heightROI+"_slice."+ i, pixelsROI);
                                            }
                                      }    
                                    
                                    
                                //*************************************************        
                                // war orginal  stack.addSlice("microCT-Import_by_KH_w_"+widthROI+"_h_"+heightROI+"_slice."+ i, pixelsROI);
                                }


				skip = fi.gapBetweenImages;
				IJ.showProgress((double)i/fi.nImages);
			}
			is.close();
		}
		catch (Exception e) {
			IJ.log("" + e);
		}
		catch(OutOfMemoryError e) {
			IJ.outOfMemory(fi.fileName);
			stack.trim();
		}
		IJ.showProgress(1.0);
		if (stack.getSize()==0)
			return ;
		if (fi.sliceLabels!=null && fi.sliceLabels.length<=stack.getSize()) {
			for (int i=0; i<fi.sliceLabels.length; i++)
				stack.setSliceLabel(fi.sliceLabels[i], i+1);
		}
		ImagePlus imp = new ImagePlus(fi.fileName, stack);
                cal = imp.getCalibration();
                calOrg = cal.copy();
 
		if (fi.info!=null)
			imp.setProperty("Info", fi.info);
		imp.show();
		imp.setFileInfo(fi);
                System.out.println("after imp.show() -> x: "+fi.pixelWidth+" ; y: "+fi.pixelHeight);
                
                 /*
                cal.pixelWidth = el_size_mm_x;
                cal.pixelHeight = el_size_mm_y;
                cal.pixelDepth = el_size_mm_z; 
                cal.setUnit("mm");
                */
                   
                // if (metricCalibrationOrPixels==true) imp.setCalibration(cal);
                
		if (metricCalibrationOrPixels==true) {
                    cal.pixelWidth = (double)el_size_mm_x;
                    cal.pixelHeight = (double)el_size_mm_y;
                    cal.pixelDepth = (double)el_size_mm_z; 
                    cal.setUnit("mm");
                    imp.setCalibration(cal);
                }
                else {
                    cal.pixelWidth = 1.0D;
                    cal.pixelHeight = 1.0D;
                    cal.pixelDepth = 1.0D; 
                    cal.setUnit("pixel");
                    imp.setCalibration(cal); 
                }
                 
                 
                 
                 ImageProcessor ip = imp.getProcessor();
		if (ip.getMin()==ip.getMax())  // find stack min and max if first slice is blank
			setStackDisplayRange_kh(imp);
		IJ.showProgress(1.0);
		return;
	}



        void setStackDisplayRange_kh(ImagePlus imp) {
		ImageStack stack = imp.getStack();
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		int n = stack.getSize();
		for (int i=1; i<=n; i++) {
			IJ.showStatus("Calculating stack min and max: "+i+"/"+n);
			ImageProcessor ip = stack.getProcessor(i);
			ip.resetMinAndMax();
			if (ip.getMin()<min)
				min = ip.getMin();
			if (ip.getMax()>max)
				max = ip.getMax();
		}
		imp.getProcessor().setMinAndMax(min, max);
		imp.updateAndDraw();
	}



        /** *********************************************************************** **/
        /**
        aus ImageReader.java:
	Skips the specified number of bytes, then reads an image and
	returns the pixel array (byte, short, int or float). Returns
	null if there was an IO exception. Does not close the InputStream.
	*/
	public short[] readPixels_kh(FileInputStream in, long skipCount) {
		this.skipCount = skipCount;
		showProgressBar = false;
		pixels = readPixels_kh(in);
		if (eofErrorCount>0)
			return null;
		else
			return pixels;
	}

        /**
	Reads the image from the InputStream and returns the pixel
	array (byte, short, int or float). Returns null if there
	was an IO exception. Does not close the InputStream.
	*/
	public short[] readPixels_kh(FileInputStream in) {
		try {

                        bytesPerPixel = 2;
                        skip_kh(in);
                        return read16bitImage_kh(in);


		}
		catch (IOException e) {
			IJ.log("" + e);
			return null;
		}
	}


 /** *********************** this is the central import routine *********************************** **/
 // there is still room to reduce code bits which are not really necessary for the ISQ-Import.

        /** Reads a 16-bit image. Signed pixels are converted to unsigned by adding 32768. */
	short[] read16bitImage_kh(FileInputStream in) throws IOException {
		int pixelsRead;
		byte[] buffer = new byte[bufferSize];
		pixels = new short[nPixels];
		int totalRead = 0;
		int base = 0;
		int count, value;
		int bufferCount;

		while (totalRead<byteCount) {
			if ((totalRead+bufferSize)>byteCount)
				bufferSize = byteCount-totalRead;
			bufferCount = 0;

			while (bufferCount<bufferSize) { // fill the buffer
				count = in.read(buffer, bufferCount, bufferSize-bufferCount);
				if (count==-1) {
					eofError();
					if (fi.fileType==FileInfo.GRAY16_SIGNED)
						for (int i=base; i<pixels.length; i++)
							pixels[i] = (short)32768;
					return pixels;
				}
				bufferCount += count;
			}
			totalRead += bufferSize;
			showProgress((double)totalRead/byteCount);
			pixelsRead = bufferSize/bytesPerPixel;
			if (fi.intelByteOrder) {
				for (int i=base,j=0; i<(base+pixelsRead); i++,j+=2)
						pixels[i] = (short)((((buffer[j+1]&0xff)<<8) | (buffer[j]&0xff))+32768);

			} else {
				for (int i=base,j=0; i<(base+pixelsRead); i++,j+=2)
						pixels[i] = (short)((((buffer[j]&0xff)<<8) | (buffer[j+1]&0xff))+32768);
				}
			base += pixelsRead;
		}
		return pixels;
	}


        	void skip_kh(FileInputStream in) throws IOException {
                    
                // I count, how often this routine is used:
                // System.out.println("skip_kh called");
                // answer: called for every slice
                    
		if (skipCount>0) {
			long bytesRead = 0;
			int skipAttempts = 0;
			long count;
			while (bytesRead<skipCount) {
				count = in.skip(skipCount-bytesRead);
				skipAttempts++;
				if (count==-1 || skipAttempts>5) break;
				bytesRead += count;
				//IJ.log("skip: "+skipCount+" "+count+" "+bytesRead+" "+skipAttempts);
			}
		}
		byteCount = width*height*bytesPerPixel;
		
		nPixels = width*height;
		bufferSize = byteCount/25;
		if (bufferSize<8192)
			bufferSize = 8192;
		else
			bufferSize = (bufferSize/8192)*8192;
	}

        void eofError() {
		eofErrorCount++;
	}

        private void showProgress(double progress) {
		if (showProgressBar)
			IJ.showProgress(progress);
	}

  /*      
  void setCalibration(ImagePlus imp) {
		  // A signed 16-bit image in ImageJ is represented by an unsigned image
		  // with 32768 added and a calibration function (y=-32768+x) that subtracts 32768.

		/* KH: 4.3.08 - temporarily deactivated: now I have the same values for the downsampled and the original short images
                 *              if I would subtract 32768 ... then I would have the right values even compared
                 *              with the float images (after division by 4096)

                 
                 if (fi.fileType==FileInfo.GRAY16_SIGNED) {
			if (IJ.debugMode) IJ.log("16-bit signed");
			double[] coeff = new double[2];
			coeff[0] = -32768.0;
			coeff[1] = 1.0;
 			imp.getLocalCalibration().setFunction(Calibration.STRAIGHT_LINE, coeff, "gray value");
		}*/
/*
               // Properties props = decodeDescriptionString_kh();
		Calibration cal = imp.getCalibration();
		
			cal.pixelWidth = fi.pixelWidth;
			cal.pixelHeight = fi.pixelHeight;
			cal.pixelDepth = fi.pixelDepth;
			cal.setUnit(fi.unit);
		


		if (fi.frameInterval!=0.0)
			cal.frameInterval = fi.frameInterval;

		return;

	}
*/

}




