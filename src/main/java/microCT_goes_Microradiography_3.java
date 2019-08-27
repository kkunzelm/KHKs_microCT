/*
 * microCT_goes_Microradiography_3.java
 *
 *
 * Todos/Done:
 *
 * todo: 1 bis n rechter Mausclick löscht letzt Markierung (Einzelpunkt, oder ganz Integral oder ganz Referenz, z. B. wenn innerhalb von Rechteck)
 *          - Problem 
 * todo: Markieren von subsurface lesion und body of lesion z. B. mit Alt oder Shift gedrückt (siehe Krüger C:\Programme\Java\Buecher\Krueger\k100188.html und früher)
 * todo: deltaZ ... Überlegen, wie man vorgeht, wenn Flächenanteile über oder unter Kurve sind
 * todo: body of lesion, lesion depth, subsurface lesion integrieren
 * todo: automatisch "pixelWidthofVoxelElement" aus fileinfo auslesen!!
 * todo: reset scheint mir im Moment gar nicht erforderlich
 * todo: cool wäre a la live histogram eine Live-Kurve, oder ein redraw button mit neuer ROI
 * todo: benötigt man bei Average die Darstellung und Umkodierung in Prozent????
 * todo: GUI wieder richtig programmieren (save, load, important data,...)
 * todo: Abfragen: Balken grau und grün dürfen nicht länger als letzer Punkt der Kurve sein
 * todo: Abfrage: grüne und graue Balken dürfen sich nicht überlappen
 * todo: bei Klick mit rechter Mousetaste - Punkt zurücknehmen
 * 
 * done: average integrieren 
 * done: deltaZ integrieren
 * done: Visualisierung des Integrals ergänzen
 * done: automatische y-Achsen-Skalierung ergänzen: ypl vs. yScale
 * done: Bei average: Routine so ändern, dass ein zweiter Punkt als Ende des Intervalls verwendet wird
 * done: Neudarstellung mit 20 % als Anfang beenden
 * done: eigene Eckpunkte für das Integral definieren!
 * done: Bereich für Referenzebene und Bereich für deltaZ werden optisch dargestellt
 * done: public void mousePressed(MouseEvent e) stark vereinfacht
 * done: calculateAverageNew ergänzt
 * done: calculateAverage aus paint Methode eliminieren
 
 * Last Change: 21.8.09 Added KHKs Copy Protection
 * Created on 19. Juli 2006, 16:43
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 * 
 * done: 28.3.08: openFile deactivated, left as a comment
 * done: 28.3.08: calculateIntersect, calculateGradient, calculateC deactivated, left as a comment
 * done: 28.3.08: calculateSSLayer, calculateLesionBody deactivated, left as a comment
 * done: 28.3.08: import of unused classes commented out
 * done: 28.3.08: lesionBodyX, lesionBodyY, subSurfaceLayerX, subSurfaceLayerY commented out
 * 
 * done: 30.3.08: copy important code to the clipboard - Idea behind: open an excel file, copy for the first line the header, then copy for every evaluation the final results
 * 			it is possible to export the profile
 * done: 8.4.08: old code which was commented removed. Look in version _2 for details on routines like import etc.
 *
 * todo: In Zukunft - deltaZ - getrennte Berechnung für positive und negative und absolute Flächen.
 * todo: import the profile, add important points like startIntegral, end of Integral, start of evaluation area ... calculate the results again.
 */
import ij.ImagePlus;
import ij.gui.ProfilePlot;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.text.TextPanel;
import ij.io.FileInfo;
import java.util.Locale;   //java.util.Locale, java.text.NumberFormat
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.*;
import java.awt.BorderLayout;


public class microCT_goes_Microradiography_3
    implements PlugInFilter
{
    
    static ImagePlus orgImg;
    Graph graph;
    double originalData[];
    double yAxis[];
   
    int points;
    double xScale;
    double yScale;
    int startX;
    int startY;
    Point startEnamel;
    Point startReferenceLevel;     //KH 
    Point endReferenceLevel;       //KH 
    Point startIntegral;           //KH 
    Point endIntegral;             //KH 
    int temp2switch, startRL, endRL, startI, endI; //RL = ReferenceLevel, I = integral;
    int numberOfClicks;             // wieoft wurde mit der Maus geklickt, um Punkte zu markieren
    double deltaZ;
    int noOfPointsInIntegral;
    int startPointIntegral;
    int endPointIntegral;
    double avgMineralLoss;
    Point startGraph;
    int actualX;
    int actualY;
    JFrame frame;
    JMenu menu;
    JMenuBar bar;
    JMenuItem resetItem;
    // JMenuItem openfile;
    JMenuItem savefile;
    JMenuItem table;
    JMenuItem export;
    JMenuItem beenden;
    //KH: JButton reset;
    java.util.List data;
    int firstEnamelPoint;
    //double subSurfaceLayerX;
    //double subSurfaceLayerY;
    //double lesionBodyX;
    //double lesionBodyY;
    double pixelWidthOfVoxelElement;
    double lesionWidth;
    double lesionDepth;
    boolean chooseStart;
    public int startProfile;
    public int readFirstEnamelPoint;
    private boolean avgCalculated;
    private boolean deltaZCalculated;
    int [] xPoints;
    int [] yPoints;
    int imageType;
    String voxelSizeUnit;
    String textForTextPanel;
    String headerForTextPanel;
    int numberOfXPixels;
   
    private double calculatedAvg;

     public int setup(String arg, ImagePlus imp)
    {
        orgImg = imp;
        FileInfo fi = imp.getFileInfo();
        
        pixelWidthOfVoxelElement = fi.pixelWidth;
        
        System.out.println("PixelWidth: "+fi.pixelWidth+"; VoxelUnit: "+fi.unit);
        
        imageType = fi.fileType;
        
        if (fi.unit == null){
            voxelSizeUnit = "pixel";
        }
        else {
        voxelSizeUnit = fi.unit;
        }
        // System.out.println("fileType: "+imageType); // 8bit = 0, 16bit = 2, 32bit = 4
        return DOES_8G+DOES_16+DOES_32+ROI_REQUIRED;

    }
    
     public void run(ImageProcessor processor)
    {   
        //*********************Begin KHKs Copy Protection *********************

        //if (!DateAndMacAddrCheck.mainOfKHKsCopyProctection()){
        //    System.out.println("I am so sorry...");
        //    return;
        //}
        //else {
        //   System.out.println("Heureka ...");
        //}
        //***********************End KHKs Copy Protection **********************


        
        frame = new JFrame("Prof. Kunzelmann's MicroCT_goes_Microradiography:  " + orgImg.getTitle());

        initGUI();
        //ij.gui.Roi roi = orgImg.getRoi();
        ProfilePlot plot = new ProfilePlot(orgImg);
        originalData = plot.getProfile();
        // resetData();
        
        // Graph ist die innere Klasse zur Darstellung des Diagramms
        graph = new Graph();
        resetData();
        /**
        Container c = meinFrame.getContentPane();
        c.setLayout( new BorderLayout() );
        JPanel j = new JPanel( new FlowLayout() );
        c.add( j, BorderLayout.NORTH );
        j.add( new JButton( "NORTH1" ) );
        **/
        chooseStart = true;
        //frame.getContentPane().add(graph, "Center");  // graph wird in den hier verwalteten JFrame geschrieben
        Container c = frame.getContentPane();
        c.setLayout( new BorderLayout());
        c.add(graph,BorderLayout.CENTER);
        c.add( new JLabel( "1. Select a reference plan = 2 mouse clicks, 2. Select the range for the evaluation integral = 2 mouse clicks"), BorderLayout.SOUTH );

        frame.setBounds(100, 70, 900, 800);
        frame.pack();
        frame.setVisible(true);
        
    } 

    public microCT_goes_Microradiography_3()
    {
        graph = null;
        originalData = new double[0];
        yAxis = new double[0];
 
        points = 0;
        xScale = 0;
        yScale = 0;
        startEnamel = null;
        startReferenceLevel = null;    //KH
        endReferenceLevel = null;      //KH
        startIntegral = null;          //KH
        endIntegral = null;            //KH
        temp2switch = 0; startRL = 0; endRL = 0; startI = 0; endI = 0;  //KH
        deltaZ = 0;
        avgMineralLoss = 0;
        startGraph = null;
        frame = null;
        menu = new JMenu("Datei");
        bar = new JMenuBar();
        resetItem = new JMenuItem("Reset");
        // KH: openfile = new JMenuItem("Open File");
        savefile = new JMenuItem("Save File");
        table = new JMenuItem("TableView");
        export = new JMenuItem("Export");
        beenden = new JMenuItem("Beenden");
        //KH: reset = new JButton("Reset");
        data = new ArrayList();
        firstEnamelPoint = -1;
       
        lesionWidth = 0.0D;
        lesionDepth = 0.0D;
        chooseStart = false;
        readFirstEnamelPoint = -1;
        avgCalculated = false;
        deltaZCalculated = false;
        
        noOfPointsInIntegral = 0;
        startPointIntegral = 0;
        endPointIntegral = 0;
        calculatedAvg = 0.0D;
        
        
    }
    

    private void initGUI()
    {
        frame.setJMenuBar(bar);
        Action action = new AbstractAction("Beenden") {

            public void actionPerformed(ActionEvent e)
            {
                frame.dispose();
            }

        };
        
       
        
        Action export = new AbstractAction("Export") {

            public void actionPerformed(ActionEvent e)
            {
                exportData();
            }

        };
        Action importantData = new AbstractAction("Show Table") {

            public void actionPerformed(ActionEvent e)
            {
                showImportantData();
            }

        };
        /*
        Action open = new AbstractAction("Open File...") {

            public void actionPerformed(ActionEvent e)
            {
                openFile("C:\\microct\\");
            }

        };
        */
        Action save = new AbstractAction("Save File...") {

            public void actionPerformed(ActionEvent e)
            {
                saveAs("C:\\microct\\");
            }

        };
        bar.add(menu);
        menu.add(table);
        //KH: menu.add(openfile);
        menu.add(savefile);
        export.setEnabled(true);
        menu.add(beenden);
        beenden.setAction(action);
        table.setAction(importantData);
        //KH: reset.setAction(resetAction);
        //KH: openfile.setAction(open);
        savefile.setAction(save);
    }

    String saveAs(String dir) // muss da ein string zurückgegeben werden ? falls nein, dann wäre return "" nicht nötig und es würde return allein reichen 
    {
        JFileChooser c;
        FileOutputStream fos;
        File file;
        byte bytedata[];
        c = new JFileChooser(new File(dir));
        fos = null;
        file = null;
        bytedata = (byte[])null;
        StringBuffer output = new StringBuffer();
        for(int i = 0; i < originalData.length; i++)
            output.append(originalData[i]).append(i != originalData.length - 1 ? "#" : "");

        output.append(";");
        output.append(startProfile).append(";");
        output.append(startGraph == null ? " " : startGraph.getX() + "#" + startGraph.getY()).append(";");
        output.append(firstEnamelPoint).append(";");
        output.append(startEnamel == null ? " " : startEnamel.getX() + "#" + startEnamel.getY()).append(";");
        bytedata = output.toString().getBytes();
        
        //*************************************
        
        if(c.showSaveDialog(frame) != 0){
             c = null;
             return ""; //wohin??
        }
        else{
            file = c.getSelectedFile();
            if(file != null) {
                try{
                    fos = new FileOutputStream(file);
                    fos.write(bytedata);
                    fos.flush();
                    c = null;
                    return "";
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(frame, "Error, file not saved");
                    c = null;
                    return "";
                }
            
            }
         
        return ""; //kh unsauber 
        }
    }
/*

// for future purposes - this was the original import routine. Well done but no longer fitting to my variables.

    void openFile(String dir)
    {
        JFileChooser c;
        FileInputStream fis;
        File file;
        c = new JFileChooser(new File(dir));
        fis = null;
        file = null;
        //byte bytedata[] = (byte[])null;
        
        if(c.showOpenDialog(frame) != 0){
            c = null;
            return;
        }
        else {
            file = c.getSelectedFile();
        if(file == null)
        {
            JOptionPane.showMessageDialog(frame, "No file selected.");
            return;
        }
        // System.out.println(file + ":" + file.getAbsolutePath());
        
        try{
        fis = new FileInputStream(file);
        
        byte bytedata[] = new byte[(int)file.length()];
        
        fis.read(bytedata);
        
        String input = new String(bytedata);
        ArrayList org = new ArrayList();
        ArrayList data = new ArrayList();
        StringTokenizer st = new StringTokenizer(input, ";");
        
        for(int counter = 0; st.hasMoreElements(); counter++)
        {
            String temp = st.nextToken();
            if(counter == 0)
            {
                for(StringTokenizer s1 = new StringTokenizer(temp, "#"); s1.hasMoreTokens(); org.add(s1.nextToken()));
                originalData = new double[org.size()];
                for(int i = 0; i < org.size(); i++)
                    originalData[i] = Double.parseDouble((String)org.get(i));

            }
            if(counter == 1)
                startProfile = Integer.parseInt(temp);
            if(counter == 2)
            {
                int index = temp.indexOf("#");
                // System.out.println(index + "_counter1");
                if(index == -1 || temp.trim().length() == 0)
                {
                    startGraph = null;
                } else
                {
                    int xp = (int)Double.parseDouble(temp.substring(0, index));
                    int yp = (int)Double.parseDouble(temp.substring(index + 1));
                    startGraph = new Point(xp, yp);
                    // System.out.println(startGraph + "_" + (int)((double)(startGraph.x - 40) / xScale));
                    chooseStart(startProfile);
                    chooseStart = true;
                    graph.repaint();
                }
            }
            if(counter == 3)
                readFirstEnamelPoint = Integer.parseInt(temp);
            if(counter == 4)
            {
                int index = temp.indexOf("#");
                // System.out.println(index + "_counter2");
                if(temp.trim().length() == 0 || index == -1)
                {
                    startEnamel = null;
                } else
                {
                    int xp = (int)Double.parseDouble(temp.substring(0, index));
                    int yp = (int)Double.parseDouble(temp.substring(index + 1));
                    startEnamel = new Point(xp, yp);
                    // System.out.println(startEnamel + "_");
                    export.setEnabled(true);
                    graph.painted = false;
                    double avgNow = graph.calculateAverage();
                    
                    graph.calculateDeltaZ();
                    graph.calculateSSLayer();
                    graph.calculateLesionBody();
                }
            }
        }
 

        graph.repaint();
        c = null;
        return;
          
        }
        catch (Exception e){
            JOptionPane.showMessageDialog(frame, "Error, could not open file!");
            c = null;
            return ;
        }
        
    }
    }
    */
    private void exportData()
    {
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(new File("C:\\microct_data.csv"));
            StringBuffer sb = new StringBuffer();
            for(int i = 0; i < yAxis.length; i++)
                sb.append(i + ";" + String.valueOf(yAxis[i]).replace('.', ',') + ";\n");

            fos.write(sb.toString().getBytes());
            fos.flush();
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if(fos != null)
                try
                {
                    fos.close();
                }
                catch(Exception exception1) { }
        }
        return;
    }

    public void showImportantData()
    {
        final TextPanel textPanel = new TextPanel("Title");
        
  		
		/*
			Sollte auch mit ausgegeben werden:
			noOfPointsInIntegral
			startReferenceLevel = null;    //KH
			endReferenceLevel = null;      //KH
			startIntegral = null;          //KH
			endIntegral = null;            //KH
		
		*/
        
        headerForTextPanel = ("info_kh\tslice\tdeltaZ\tlesionDepth\tratio\tpixelWidth\tvoxelUnit");
        textForTextPanel = (orgImg.getTitle() + "\t" + String.valueOf(orgImg.getCurrentSlice()) + "\t" + formatDouble(graph.calculateDeltaZ()) + "\t" + formatDouble(lesionDepth) + "\t" + formatDouble(avgMineralLoss) + "\t" + pixelWidthOfVoxelElement + "\t" + voxelSizeUnit);

        textPanel.setColumnHeadings(headerForTextPanel);
        textPanel.appendLine(textForTextPanel);
       
        JFrame tw = new JFrame("Important Data for slice: " + orgImg.getCurrentSlice() + " in file: " + orgImg.getTitle());
        JButton but = new JButton("Copy to Clipboard...");
        JButton butHeader = new JButton("Copy Header to Clipboard...");
        
        but.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e1)
            {
                //System.out.println("KH:3.4.08   "+textImportantData);
                // textPanel.saveAs("");
                TextTransfer textTransfer = new TextTransfer();
                textTransfer.setClipboardContents(textForTextPanel);
            }
         });    
           
       butHeader.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e2)
            {
                //System.out.println("KH:3.4.08   "+textImportantData);
                // textPanel.saveAs("");
                TextTransfer textTransfer = new TextTransfer();
                textTransfer.setClipboardContents(headerForTextPanel);
            }
        });

        BoxLayout box = new BoxLayout(tw.getContentPane(), 1);
        tw.getContentPane().setLayout(box);
        tw.getContentPane().add(textPanel);
        tw.getContentPane().add(but);
        tw.getContentPane().add(butHeader);
        tw.setBounds(100, 300, 950, 160);
        tw.setVisible(true);
    }

  
    
    public String formatDouble(double in)
    {
        Locale.setDefault(Locale.US);
        if (imageType==4){
            DecimalFormat format = new DecimalFormat("#,##0.000");
            return format.format(in);
        }
        else if (imageType==0)
        {
            DecimalFormat format = new DecimalFormat("###0");
            return format.format(in);
        }
        else 
        {
            DecimalFormat format = new DecimalFormat("#,##0.00");
            return format.format(in);
        }
        
        
    }
    
      public String formatDoubleY(double in)
    {
        Locale.setDefault(Locale.US);
        if (imageType==4){
            DecimalFormat format = new DecimalFormat("#,##0.000");
            return format.format(in);
        }
        else 
        {
            DecimalFormat format = new DecimalFormat("###0");
            return format.format(in);
        }
      
    }
    

    public void chooseStart(int startPoint)
    {
        // System.out.println("given start point is: " + startPoint);
        double temp[] = new double[yAxis.length];
        for(int i = 0; i < yAxis.length; i++)
        {
            temp[i] = yAxis[i];
           // System.out.println("YAXIS:" + yAxis[i]);
        }

        yAxis = new double[temp.length - startPoint];
        for(int i = startPoint; i < temp.length; i++)
            yAxis[i - startPoint] = temp[i];

    }

  

    public void resetData()
    {
        
        
        // in den nächsten drei Zeilen werden die OriginalPlot-Daten aus dem Originalarray
        // in den yAxis[] Array kopiert.
        
        yAxis = new double[originalData.length];
        for(int i = 0; i < originalData.length; i++)
            yAxis[i] = originalData[i];

        chooseStart = false;
        export.setEnabled(true);
        startEnamel = null;
        startGraph = null;
        avgMineralLoss = 0.0D;
        lesionWidth = 0.0D;
        lesionDepth = 0.0D;
        avgCalculated = false;
        deltaZCalculated = false;
        noOfPointsInIntegral = 0;
        startPointIntegral = 0;
        endPointIntegral = 0;
        calculatedAvg = 0.0D;
        startReferenceLevel = null;    //KH
        endReferenceLevel = null;      //KH
        startIntegral = null;          //KH
        endIntegral = null;  
        /**
        xPoints = new int[1];
        yPoints = new int[1];
        xPoints[0]=0;
        yPoints[0]=0;
        **/
        return;
    }

     
    public class Graph extends JPanel
        implements MouseMotionListener, MouseListener{
    
        private boolean painted;
        
        Graph()
            {
                setPreferredSize(new Dimension(900, 700));
                setBorder(BorderFactory.createEtchedBorder());
                addMouseMotionListener(this);
                addMouseListener(this);
            }
        
        @Override
         public void paint(Graphics g)
        {
             
            // repaint() ruft automatisch die Methode public void update(Graphics g) auf, die den Bildschirm löscht und (evtl. nach einer Wartezeit) die paint-Methode aufruft. paint() selbst wird nicht direkt aufgerufen. 
            // Paint ist eine Methode des Containers
            // diese wird in diesem Fall hier mit einer eigenen Methode überschrieben.
             
            Color col;                          /* dies erzeugt eine Variable des Typs Color */
            col = new Color (255, 245, 185);    /* hier wird eine neue Farbe erzeugt (rot) und diese Farbe wird der Variablen Color zugewiesen */
            g.setColor(col);                    // ursprünglich: g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, 900, 800);
            
            
            startX = 70;                    // Ursprung Koordinatensystem X
            startY = 450;                   // Ursprung Koordinatensystem Y - bei Java Ursprung oben links, jetzt unten rechts
          
            
            g.setColor(Color.BLUE);                     //Linienfarbe Blau
            g.drawLine(startX, startY, 695, startY);    //x-Achse gezeichnet (scheint: 695 minus startX Pixel lang zu sein ??
            g.drawLine(startX, startY, startX, 30);     //y-Achse gezeichnet
            int countX = yAxis.length;                         // countX = Anzahl der Meßpunkte, entspricht der Länge des y-Arrays 
            numberOfXPixels = 600;    
            xScale = (double)numberOfXPixels / countX;                      // Skalierungsfaktor der X-Achse
           
            double min = 0;
            double max = 0;
            
            // pp = point position ??? , Umrechnung von Framekoord. in Koord. der xy-Achsen, bzw. Meßpunkte
            g.setColor(Color.BLACK);
            int pp = (int)((actualX - startX) / xScale);    
           
            g.setColor(Color.BLUE);
            
            // automatischen y-Achsen-Skalierung
            for(int i = 0; i < yAxis.length-1; i++) // KH: 13.4.08: warum ist hier yAxis.length -1 ... ???
            {
                min = Math.min(yAxis[i], min);          // Math.min(a,b) = kleinste von beiden Zahlen wird verwendet
                max = Math.max(yAxis[i], max);
                
            }
            
            yScale = (startY-30)/(max-min);                     // ySkalierung:  StartY minus Rand oben = 30
            

            
            // System.out.println("Berechnen x-Achsen-Tickmarks");
            // System.out.println("max: "+(float)max+ "min: "+(float)min);
            
            //*****************************************************************************************************
            // Berechnen der "tickmarks" auf der x-Achse
            
            // xTeiler und yTeiler bestimmen die Anzahl der Kategorien
            
            double xTeiler = 30D; 
            int index = 0;
            double xAxisScale = ((double)yAxis.length)/xTeiler; 
            
           
            // System.out.println("Debuginfo: breakpoint 1, xAxisScale"+xAxisScale+"  yAxisLenght: "+yAxis.length);
           
            for(double i = 0; i < yAxis.length; i += xAxisScale)
            {   
                
                // minor tickmark für jede Koordinate der y-Achse
                
                if(i != 0)
                    g.drawLine(startX + (int)((double)i * xScale), startY-1, startX + (int)((double)i * xScale), startY+1);
               
                
                
                // major tickmark ... alle fünf Koordinatenpunkte
                if(index % 5 == 0)
                    g.drawLine(startX + (int)((double)i * xScale), startY-3, startX + (int)((double)i * xScale), startY+3);
                // Beschriftung der major tickmarks
                if(index % 5 == 0) {
                    
                    g.drawString(formatDouble((double)i * pixelWidthOfVoxelElement), (startX + (int)((double)i * xScale)) - 6, startY+17); //String an 1. Position
                
                }    
                index = index + 1;
            
            }

            // g.drawString("\265m", startX + 650, startY+17);  //... Anzeige von Unit µm
            g.drawString(voxelSizeUnit, startX + 650, startY+17);
           
            
            
           // System.out.println("Berechnen y-Achsen-Tickmarks");
           // System.out.println("yScale= "+yScale+" ; min=  "+min+"  ; max= "+max);
            
            //*****************************************************************************************************
            // Berechnen der tickmarks auf der y-Achse
            int yTeiler = 20;
            if(chooseStart && startEnamel != null)                  // wenn diese Bedingungen erfüllt: Skala in Prozent, Max = 100
                max = 100D;
           
            
            double yAxisScale =     ((max - min) / yTeiler);      // wenn obige Bed. nicht erfüllt: Teile zwischen min und max in 10 Untereinheiten

            // System.out.println("yScale= "+yScale+" ; min=  "+min+"  ; max= "+max+"   yAxisScale= "+ yAxisScale);
            
            if(chooseStart && startEnamel != null)
                yAxisScale = 10;
            for(double i = 0; i < max ; i += yAxisScale)
           
// KH: 30.3.08 - ich habe die beiden if Bedingungen noch auskommentiert, im Prinzip kann man darauf verzichten.
//               im Moment klappt alles... trick war i nicht int zum machen !
//               Falls ich die if Bedingungen mal wieder haben will, lieber eine zweite int Index Variable einführen
//               d. h. ausserhalb der for Schleife int index = 0; dann in der for Schleife index = index +1; if index % (...)
     
            {
                // minor tickmarks
                if(i != 0)
                    g.drawLine(startX - 1, (int)((double)startY - (double)i * (yScale)), startX + 1, (int)((double)startY - (double)i * (yScale)));
                // major tickmarks
                //if(i % (yAxisScale * 5) == 0)
                //    g.drawLine(startX - 3, (int)((double)startY - (double)i * yScale), startX + 3, (int)((double)startY - (double)i * yScale));
                // labels for major tickmarks
                // if(i % (yAxisScale * 5) == 0)
                // System.out.println((formatDouble(i)).length()); // Annahme: ein Zeichen sei 7 pixel
                // Short Strings sind 9 Zeichen lang, LAC sind nur 4 Zeichen lang.
                // hier schöner Formatieren. Könnte man eleganter programmieren.
                
                g.drawString(formatDoubleY(i), startX-5-(formatDoubleY(i).length()*7), (int)((double)(3 + startY) - (double)i * yScale)); // i converted to string, right aligned
                  
              }

            
            //*****************************************************************
            // Einheit-Label für y-Achse
            String yAxisText = !chooseStart || startEnamel == null ? "Lin.Att.Coeff." : "Vol%";
            g.drawString(yAxisText, 5, 20);
            g.setColor(Color.BLACK);

            // System.out.println("Delta-Z-Integral visuell");
            //**************************************************************************
            // DeltaZ-Integral wird visuell dargestellt
             
             if (deltaZCalculated == true){
                xPoints = new int[noOfPointsInIntegral + 3];                    //KH: 13.04.2008 warum steht hier +2 ??
                yPoints = new int[noOfPointsInIntegral + 3];                    // weil bei einem Polygon auch die noch die linke und obere Ecke hinzukommen
                                                                                // und auch die Begrenzung rechts unten noch stimmen muss
                
                // todo löschen Array wenn 4 Mouseklicks
                
                for (int i = 0; i <= noOfPointsInIntegral; i++){                 //KH: 13.04.2008 - hier war <= gestanden
                    xPoints[i] = startX + (int)((startPointIntegral+i)*xScale);
                    yPoints[i] = startY - (int)(yAxis[startPointIntegral+i]*yScale);
                }
                
               xPoints[noOfPointsInIntegral+1]= startX + (int)((startPointIntegral+noOfPointsInIntegral)*xScale);  // Ecke oben rechts - Bei Average line
               yPoints[noOfPointsInIntegral+1]= startY - (int)(calculatedAvg*yScale*0.95);
               
               xPoints[noOfPointsInIntegral+2]= startX + (int)(startPointIntegral*xScale);  // Ecke oben links bei average line
               yPoints[noOfPointsInIntegral+2]= startY - (int)(calculatedAvg*yScale*0.95);
               
               g.setColor(Color.LIGHT_GRAY);
               g.fillPolygon(xPoints, yPoints, yPoints.length); 
             } 
            
            // System.out.println("Kreuzlinien durch Cursor");
            //****************************************************************
            //  Kreuzlinien durch Cursor 
            
            if(actualX > startX && actualX < 690 && actualY < startY && actualY > 30 )
            {   
                // System.out.println("actualX, startX, actualY und startY: "+actualX+","+startX+","+actualY+","+startY);
                g.setColor(Color.DARK_GRAY);
                g.drawLine((int)actualX, 0, (int)actualX, startY); //Draws a line, using the current color, between the points (x1, y1) and (x2, y2) in this graphics context's coordinate system.
                g.drawLine(startX, (int)actualY, 690, (int)actualY);
                
                // Auslesen der Cursor-Koordinaten und anzeigen unter Diagram
                int startYText = startY + 60;
                g.drawString("Current Point X [in screen pixels]: " + formatDouble(actualX), 530, startYText + 30);
                g.drawString("Current Point Y [in screen pixels]: " + formatDouble(actualY), 530, startYText + 50);
                g.setColor(Color.BLACK);
            }
            if(actualX > startX && actualY < startY && actualY > 30 && actualX < 690 && pp >= 0 && pp < yAxis.length)
            {
               // Auslesen der Cursor-Koordinaten und anzeigen rechts neben dem Cursor
               // es wird jeweils die x- und y-Koordinate des Messpunktes links vom Cursor-Kreuz wiedergeben.
               // die Verbindungslinien werden nicht interpoliert.
                
                g.setColor(Color.BLACK);
                g.drawString("(" + formatDouble(pp*pixelWidthOfVoxelElement) + "," + formatDouble(yAxis[pp]) + ")", (actualX + 5), (actualY - 5));

                }
            
            // System.out.println("Darstellen der Plotline der xy-Werte");
            //*****************************************************************************************************
            // in diesem Block wird die Plotlinie der xy-Werte dargestellt, also die Kurve der Daten gezeichnet
            g.setColor(Color.BLUE);
           
            for(int i = 0; i < yAxis.length-1; i++)       {
                g.setColor(Color.BLUE);
                
                // Linie der Kurve selbst
                g.drawLine(startX + (int)((double)i * xScale), startY - (int)(yAxis[i] * yScale), startX + (int)((double)(i + 1) * xScale), startY - (int)(yAxis[i + 1] * yScale));
                // Punkte auf der Kurve
                if (i==0){
                    g.fillRect(-1 + startX + (int)((double)i * xScale), (-1 + startY) - (int)(yAxis[i] * yScale), 3, 3);
                }  
                else {
                    g.fillRect(-1 + startX + (int)((double)(i+1) * xScale), (-1 + startY) - (int)(yAxis[i+1] * yScale), 3, 3);
                }
                    
            }
            
            
            //*****************************************************************************************************
            // KH - zeichnet farbige Rechtecken mit den Koordinaten der 4 Mouse-clicks
           
            g.setColor(Color.GREEN);
            if (startReferenceLevel != null && endReferenceLevel != null){
                g.drawLine(startRL, 30, startRL, startY);
                g.drawLine(endRL, 0, endRL, startY);
                g.fillRect(startRL, 0, endRL-startRL, 30);
            }            
                
            
            g.setColor(Color.LIGHT_GRAY);
            if (startIntegral != null && endIntegral != null){
                //g.drawLine(startI, 30, startI, startY);
                //g.drawLine(endI, 0, endI, startY);
                //g.fillRect(startI, 0, endI-startI, 30);
                g.drawLine(startX+(int)(startPointIntegral*xScale),30,startX+(int)(startPointIntegral*xScale),startY);
                g.drawLine(startX+(int)(endPointIntegral*xScale),0,startX+(int)(endPointIntegral*xScale),startY);
                g.fillRect(startX+(int)(startPointIntegral*xScale),0,(int)((endPointIntegral-startPointIntegral)*xScale),30);
                
            }
         
            //*****************************************************************************************************
            // Anzeigen der Ergebnisse in Textform:
             g.setColor(Color.BLACK);
             if (avgCalculated == true){
             int startYText = startY + 60;
                g.drawString("Average ReferenceLevel 100%: " + formatDouble(calculatedAvg), 130, startYText + 30);
                g.drawString("Average ReferenceLevel  95%: " + formatDouble(calculatedAvg*0.95), 130, startYText + 50);
                int lineAvg100 = (int)(startY-(calculatedAvg*yScale));  // Visualisierung von Average
                g.drawLine(startX, lineAvg100, startX+650, lineAvg100 );
                int lineAvg95 = (int)(startY-(calculatedAvg*yScale)*0.95); // Average minus 5 %
                int lineAvg20 = (int)(startY-(calculatedAvg*yScale)*0.20); // Average 20% Niveau
                g.setColor(Color.RED);
                g.drawLine(startX, lineAvg95, startX+650, lineAvg95 ); 
                g.drawLine(startX, lineAvg20, startX+650, lineAvg20 );
                g.setColor(Color.BLACK);
                g.drawString("Integral('deltaZ') [(sum[i] += avg*0.95 - yAxis[i])*pixel width]: " + deltaZ, 130, startYText + 70);
                g.drawString("width-Integral [noOfPoints*pixel width]: " + (((endI - startI) / xScale)*pixelWidthOfVoxelElement), 130, startYText + 90);
                g.drawString("avgMineralLoss=Integral/width-Integral [Greylevel*pixel width]: " + avgMineralLoss, 130, startYText + 110);
                g.drawString("Pixel width (assumed to be equal in x,y,z): "+pixelWidthOfVoxelElement+" "+voxelSizeUnit, 130, startYText + 130);
             }
        }
        
        
         
        double calculateAverage()
        {
           
                double sum = 0.0D;
                
                // StartRL und endRL sind in Pixelkoordinate des Frames
                // hier erfolgt die Umrechnung in Index-Werte
                int startPointAverage = (int)((double)(startRL - startX) / xScale); 
                int endPointAverage = (int)((double)(endRL - startX) / xScale); 
               
                for(int i = startPointAverage; i < endPointAverage; i++){
                    sum += yAxis[i];
                    // ich habe mit Excel geprüft: die Werte stimmen!
                    // System.out.println(i+", "+yAxis[i]);
                    
                }
                calculatedAvg = sum / (double)(endPointAverage - startPointAverage);
               // System.out.println("CalculatedAvg "+calculatedAvg);
                return calculatedAvg;
           
        }

      
       
        
        double calculateDeltaZ()
        {
             double sum = 0.0D;
                
                // StartI und endI sind in Pixelkoordinate des Frames
                // hier erfolgt die Umrechnung in Index-Werte
                startPointIntegral = (int)((double)(startI - startX) / xScale); 
                endPointIntegral = (int)((double)(endI - startX) / xScale); 
                noOfPointsInIntegral = endPointIntegral - startPointIntegral; 
                double avg = calculateAverage();
                System.out.println("StartPointInt: "+startPointIntegral+"; endPointIntegral: "+endPointIntegral+"; noOfP: "+noOfPointsInIntegral+"; arraylength: "+yAxis.length);
                
                for(int i = startPointIntegral; i < endPointIntegral; i++){       
                    sum += avg*0.95 - yAxis[i];
                    // System.out.println( "DeltaZ i: "+i + " ; avg - yAxis: " + yAxis[i]);
                    
                }

            deltaZ = sum * pixelWidthOfVoxelElement;
            lesionDepth = noOfPointsInIntegral * pixelWidthOfVoxelElement;
            
            System.out.println("noOfPointsInIntegra: "+noOfPointsInIntegral);
            System.out.println("pixelWidthofVoxelEle: "+pixelWidthOfVoxelElement);
            
            avgMineralLoss = deltaZ / lesionDepth;
            //System.out.println ("DeltaZ = " + deltaZ + " ; MineralLoss: " + avgMineralLoss);
            return deltaZ;
        }

        public void mouseDragged(MouseEvent mouseevent)
        {
            /** Beispielcode aus Krüger (C:\Programme\Java\Buecher\Krueger\k100188.html)
             *  wird kombiniert mit mousePressed (definiert Objekt) - mouseReleased (zeichnet und registriert Objekt) 
            int x = event.getX();
            int y = event.getY();
            if (x > actrect.x && y > actrect.y) {
                 actrect.width = x - actrect.x;
                 actrect.height = y - actrect.y;
            }
              repaint();
            **/
        }

        public void mouseMoved(MouseEvent e)
        {
            Point p = e.getPoint();
            actualX = (int)p.getX();  // aktuelle Mouseposition X
            actualY = (int)p.getY();  // aktuelle Mouseposition Y
            repaint();   // Graphic wird neu gezeichnet, sobald sich die Mouse bewegt hat
        }

        public Point checkPointPosition(Point e){
            
            // eigene Methode von mir: ich hatte das Problem, dass der Cursor auch Markierungen
            // "hinter" der Kurve erlaubt hat. Durch die Routine zur Berechnung von DeltaZ 
            // ergaben sich dann Punkte, für die kein Integral berechnet werden konnte, 
            // da der Index des Arrays für die Punkte zu gross wurde.
            // ich fange das hier ab, indem ich alle x-Koordinaten auf die Länge der 
            // Kurve beschränke.
            
            Point p = e;
            actualX = (int)p.getX();  // aktuelle Mouseposition X
            actualY = (int)p.getY();  // aktuelle Mouseposition Y
            if (actualX > numberOfXPixels + startX){
               p.setLocation((numberOfXPixels + startX) - xScale,actualY); // numberOfXPixels + startX = ist max. Wertebereich in x, xScale = 1 Punkt weniger, dadruch habe ich dann beim Zeichnen des Integrals kein Problem mit dem Index von yAxis
            }
            if (actualX < startX){
               p.setLocation(startX,actualY); // startX = ist min. Wertebereich von x
            }
            
            return p;
            
        }
        
        public void mousePressed(MouseEvent e)
        {
           
                // Beginn der Programmierung für Markierungen zur Auswertung
                // numberOfClicks = numberOfClicks
                if (numberOfClicks >= 4) {
                        numberOfClicks = 0;
                        resetData();
                       
                }
                
                
                // mit der linken Maustaste kann man die Referenzebene und das Flächenintegral auswählen
                if(SwingUtilities.isLeftMouseButton(e)){
              
                numberOfClicks += 1;
              
                
                if (numberOfClicks == 1){
                    startReferenceLevel = checkPointPosition(e.getPoint());
                    endReferenceLevel = null;
                    System.out.println("*************************noOfClicks1 "+startReferenceLevel.x);
                }
                else if (numberOfClicks == 2){
                    endReferenceLevel = checkPointPosition(e.getPoint());
                    System.out.println("*************************noOfClicks2 "+endReferenceLevel.x);
                }
                 else if (numberOfClicks == 3){
                    startIntegral = checkPointPosition(e.getPoint());
                    endIntegral = null;
                    System.out.println("*************************noOfClicks3 "+startIntegral.x);
                }
                 else if (numberOfClicks == 4){
                    endIntegral = checkPointPosition(e.getPoint());
                    System.out.println("*************************noOfClicks4 "+endIntegral.x);
                }

                 }
                // Vorbereitung für weitere Berechnungen: in aufsteigender Grösse sortieren
                if (startReferenceLevel != null && endReferenceLevel != null){
                        if (endReferenceLevel.x < startReferenceLevel.x){
                            startRL = endReferenceLevel.x;
                            endRL = startReferenceLevel.x;
                        }
                        else {
                            startRL = startReferenceLevel.x;
                            endRL = endReferenceLevel.x;
                         }
                }        
                if (startIntegral != null && endIntegral != null){
                         if (endIntegral.x < startIntegral.x){
                            startI = endIntegral.x;
                            endI = startIntegral.x;
                        }
                        else {
                            startI = startIntegral.x;
                            endI = endIntegral.x;
                         }
                }
                // 
               
               if (startReferenceLevel != null && endReferenceLevel != null){
                    if (avgCalculated == false){
                        calculateAverage();
                    }
                    avgCalculated = true;
               }
                
               if (startIntegral != null && endIntegral != null){
                    if (deltaZCalculated == false){
                        calculateDeltaZ();
                    }
                    deltaZCalculated = true;
                    
               }
               
               
                
                
                repaint();
           
        }

        public void mouseClicked(MouseEvent mouseevent)
        {
        }

        public void mouseEntered(MouseEvent mouseevent)
        {
        }

        public void mouseExited(MouseEvent mouseevent)
        {
        }

        public void mouseReleased(MouseEvent mouseevent)
        {
        }

        
       
        
             
    }
}



