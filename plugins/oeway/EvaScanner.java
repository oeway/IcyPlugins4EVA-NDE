package plugins.oeway;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopeCore;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopePluginAcquisition;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopeSequence;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.Tools.ImageGetter;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.Tools.StageMover;
import icy.gui.frame.progress.AnnounceFrame;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.ROI2D;
import icy.sequence.Sequence;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import javax.swing.JButton;

import plugins.adufour.ezplug.*;
import icy.gui.viewer.Viewer;

/**
 * Plugin for 
 * 
 * @author Wei Ouyang
 * 
 */
public class EvaScanner extends EzPlug implements EzStoppable, ActionListener
{
	

	
    /** CoreSingleton instance */
    MicroscopeCore core;
    Sequence currentSequence = null;
	EzButton 					markPos;
	EzButton 					getPos;	
	EzButton 					gotoPostion;	
	
	EzVarDouble					posX;
	EzVarDouble					posY;	
	EzButton 					generatePath;
	EzVarDouble					stepSize;
	EzVarSequence 				scanMapSeq;
	EzVarFile					generateFilePath;	
	
	EzVarDouble					scanSpeed;
	EzVarFile					pathFile;
	EzVarText					note;
	EzVarFolder					targetFolder;

	String lastSeqName = "";
	String currentSeqName = "";	
	String xyStageLabel ="";
	String xyStageParentLabel ="";
	
	String picoCameraLabel ="";
	String picoCameraParentLabel ="";		
	long rowCount =100;
	long frameCount = 1;
	
	// some other data
	boolean						stopFlag;
	
	@Override
	protected void initialize()
	{
		// 1) variables must be initialized
		scanMapSeq = new EzVarSequence("Scan Map Sequence");
		stepSize = new EzVarDouble("Step Size");
		markPos = new EzButton("Mark Position", this);
		gotoPostion = new EzButton("Goto Position", this);		
		generatePath = new EzButton("Generate Path", this);
		getPos = new EzButton("Get Position", this);		
		generateFilePath = new EzVarFile("Save Path", null);
		posX = new EzVarDouble("X");
		posY = new EzVarDouble("Y");		
		
		scanSpeed = new EzVarDouble("Scan Speed");
		pathFile = new EzVarFile("Path File", null);
		
		note = new EzVarText("Scan Note", new String[] { "Test" }, 0, true);
		
		targetFolder = new EzVarFolder("Target Folder", null);
		
		// 2) and added to the interface in the desired order
		
		// let's group other variables per type
		stepSize.setValue(1.0);
		
		EzGroup groupMark = new EzGroup("Mark", getPos,posX,posY,gotoPostion,markPos);
		super.addEzComponent(groupMark);			
		
		EzGroup groupScanMap = new EzGroup("Scan Map", scanMapSeq,stepSize,generateFilePath,generatePath);
		super.addEzComponent(groupScanMap);	
		
		
		EzGroup groupSettings = new EzGroup("Settings", pathFile, scanSpeed,note); //TODO:add targetFolder
		super.addEzComponent(groupSettings);
		
		core = MicroscopeCore.getCore();
		
	}	
	protected boolean waitUntilComplete()
	{
		if(xyStageLabel.equals("")){
			 xyStageLabel = core.getXYStageDevice();
	    try {
		   xyStageParentLabel = core.getParentLabel(xyStageLabel);
		} catch (Exception e1) {
			new AnnounceFrame("XY Stage Error!");
			return false;
		} 
		}
  		String status = "";
  		//wait until movement complete
  		while(!stopFlag){
  			try {
				status = core.getProperty(xyStageParentLabel, "Status");
			} catch (Exception e) {
				return false;
			}
  			if(status.equals("Idle"))
  				break;
  			else if(!status.equals("Run"))
  				break;
  			Thread.yield();
  		}
  		if(!status.equals( "Idle") && !stopFlag) // may be error occured
  		{
  			new AnnounceFrame("XY stage status error!");
  			return false;
  		}
  		return true;
		
	}
	protected boolean snap2Sequence()
	{
		
        IcyBufferedImage capturedImage;
        if (core.isSequenceRunning())
            capturedImage = ImageGetter.getImageFromLive(core);
        else
            capturedImage = ImageGetter.snapImage(core);
        
        if (capturedImage == null)
        {
            new AnnounceFrame("No image was captured");
            return false;
        }
        
        if(!lastSeqName.equals( currentSeqName) || currentSequence == null )
        {
        	if(currentSequence !=null)
        	{
//        		if(targetFolder.getValue() == null){
//        			new AnnounceFrame("Please select a target folder to store data!");
//        		}
        	    //TODO:save file here
        	}
	        MicroscopeSequence s = new MicroscopeSequence(capturedImage);
	        Calendar calendar = Calendar.getInstance();
	        Icy.getMainInterface().addSequence(s);
	        s.setName(currentSeqName + "__" + calendar.get(Calendar.MONTH) + "_" + calendar.get(Calendar.DAY_OF_MONTH) + "_"
	                + calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.HOUR_OF_DAY) + "_"
	                + calendar.get(Calendar.MINUTE) + "_" + calendar.get(Calendar.SECOND));
	        currentSequence = s;
	        lastSeqName = currentSeqName;
        }
        else
        {
            try
            {
            	currentSequence.addImage(((Viewer) currentSequence.getViewers().get(0)).getT(), capturedImage);
            }
            catch (IllegalArgumentException e)
            {
                String toAdd = "";
                if (currentSequence.getSizeC() > 0)
                    toAdd = toAdd
                            + ": impossible to capture images with a colored sequence. Only Snap C are possible.";
                new AnnounceFrame("This sequence is not compatible" + toAdd);
                return false;
            }
            catch(IndexOutOfBoundsException e2)
            {
            	if(currentSequence !=null)
            	{
//            		if(targetFolder.getValue() == null){
//            			new AnnounceFrame("Please select a target folder to store data!");
//            		}
            	    //TODO:save file here
            	}
    	        MicroscopeSequence s = new MicroscopeSequence(capturedImage);
    	        Calendar calendar = Calendar.getInstance();
    	        Icy.getMainInterface().addSequence(s);
    	        s.setName(currentSeqName + "__" + calendar.get(Calendar.MONTH) + "_" + calendar.get(Calendar.DAY_OF_MONTH) + "_"
    	                + calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.HOUR_OF_DAY) + "_"
    	                + calendar.get(Calendar.MINUTE) + "_" + calendar.get(Calendar.SECOND));
    	        currentSequence = s;
    	        lastSeqName = currentSeqName;
    	        
            }
        }
        return true;
		
	}
	@Override
	protected void execute()
	{
		
		int cpt = 0;
		stopFlag = false;
		lastSeqName = "";
		// main plugin code goes here, and runs in a separate thread
		if(pathFile.getValue() == null){
			new AnnounceFrame("Please select a path file!");
			return;
		}
		
	   xyStageLabel = core.getXYStageDevice();
	   
	   try {
		   xyStageParentLabel = core.getParentLabel(xyStageLabel);
		} catch (Exception e1) {
			new AnnounceFrame("Please select 'EVA_NDE_Grbl' as the default XY Stage!");
			return;
		} 
	   
		try {
			if(!core.hasProperty(xyStageParentLabel,"Command"))
			  {
				  new AnnounceFrame("Please select 'EVA_NDE_Grbl' as the default XY Stage!");
				  return;
			  }
		} catch (Exception e1) {
			  new AnnounceFrame("XY Stage Error!");
			  return;
		}
		
		picoCameraLabel = core.getCameraDevice();
	   try {
		   picoCameraParentLabel = core.getParentLabel(picoCameraLabel);
		} catch (Exception e1) {
			new AnnounceFrame("Please select 'picoCam' as the default camera device!");
			return;
		} 
	   
		try {
			if(!core.hasProperty(picoCameraLabel,"RowCount"))
			  {
				new AnnounceFrame("Please select 'picoCam' as the default camera device!");
				  return;
			  }
		} catch (Exception e1) {
			  new AnnounceFrame("Camera Error!");
			  return;
		}		
		
//		if(targetFolder.getValue() == null){
//			new AnnounceFrame("Please select a target folder to store data!");
//			return;
//		}
	
		
		System.out.println(scanSpeed.name + " = " + scanSpeed.getValue());
		System.out.println(pathFile.name + " = " + pathFile.getValue());
		System.out.println(targetFolder.name + " = " + targetFolder.getValue());
		System.out.println(note.name + " = " + note.getValue());


		try{
		  // Open the file that is the first 
		  // command line parameter
		  FileInputStream fstream = new FileInputStream(pathFile.getValue());
		  // Get the object of DataInputStream
		  DataInputStream in = new DataInputStream(fstream);
		  BufferedReader br = new BufferedReader(new InputStreamReader(in));
		  String strLine;
		  //Read File Line By Line
		  HashMap<String , String> settings = new HashMap<String , String>();   
		  

		  super.getUI().setProgressBarMessage("Action...");
		  while ((strLine = br.readLine()) != null && !stopFlag) {
			  	// Print the content on the console
			  	strLine = strLine.trim();
			  	System.out.println (strLine);
			  	if(strLine.startsWith("(") && strLine.endsWith(")") && strLine.contains("=")){  //comment
			  		strLine = strLine.replace("(", ""); 
			  		strLine = strLine.replace(")", "");
			  		String tmp[] = strLine.split("=");
			  		tmp[0] = tmp[0].trim().toLowerCase();
			  		tmp[1] = tmp[1].trim().toLowerCase();
			  		settings.put(tmp[0],tmp[1]);
			  		
			  	   try{
				  		if(tmp[0].equals("newsequence") ){
				  			currentSeqName = tmp[1];
				  		}
				  		else if(tmp[0].equals("width")){
				  			rowCount = Integer.parseInt(tmp[1]);
				  			core.setProperty(picoCameraLabel, "RowCount",tmp[1]);
				  		}
				  		else if(tmp[0].equals("height")){
				  			frameCount = Integer.parseInt(tmp[1]);
				  		}
				  		else if(tmp[0].equals("sampleoffset")){
				  			core.setProperty(picoCameraLabel, "SampleOffset",tmp[1]);
				  		}
				  		else if(tmp[0].equals("samplelength")){
				  			core.setProperty(picoCameraLabel, "SampleLength",tmp[1]);
				  		}			  		
				  		else{
				  			new AnnounceFrame(tmp[0]+":"+tmp[1]);
				  		}
			  		}
					catch (Exception e){//Catch exception if any
						new AnnounceFrame("Error when parsing line:"+strLine);
					}
			  		
			  	}
			  	else if (strLine.startsWith("G01")){
			  		
			  		core.setProperty(xyStageParentLabel, "Command",strLine);
			  		//excute command
			  		if(!snap2Sequence()){
			  			new AnnounceFrame("Error when snapping image!");
			  			break; //exit current progress!
			  		}	
			  		if(! waitUntilComplete()){
			  			super.getUI().setProgressBarMessage("error!");
			  			return;
			  		}
			  		cpt++;
			  		super.getUI().setProgressBarValue(((cpt/frameCount)*100)%100);
			  	}
			  	else{
			  		core.setProperty(xyStageParentLabel, "Command",strLine);
			  		if(! waitUntilComplete()){
			  			super.getUI().setProgressBarMessage("error!");
			  			return;
			  		}
			  	}
			  }
		  //Close the input stream
		  in.close();
		}
		catch (Exception e){//Catch exception if any
			  super.getUI().setProgressBarMessage("error!");
			  System.err.println("Error: " + e.getMessage());
			  return;
		}
//		int cpt = 0;
//		while (!stopFlag)
//		{
//			cpt++;
//			if (cpt % 10 == 0) super.getUI().setProgressBarValue((cpt % 5000000) / 5000000.0);
//			Thread.yield();			
//		}
	}
	
	@Override
	public void clean()
	{
		// use this method to clean local variables or input streams (if any) to avoid memory leaks
		
	}
	
	@Override
	public void stopExecution()
	{
		// this method is from the EzStoppable interface
		// if this interface is implemented, a "stop" button is displayed
		// and this method is called when the user hits the "stop" button
		stopFlag = true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (((JButton)e.getSource()).getText().equals(markPos.name)) {
					
		   xyStageLabel = core.getXYStageDevice();
			try {
				double[] x_stage = {0.0};
				double[] y_stage = {0.0};
				core.getXYPosition(xyStageLabel, x_stage, y_stage);
				posX.setValue(x_stage[0]/1000.0);
				posY.setValue(y_stage[0]/1000.0);
				if(scanMapSeq != null)
				{
					IcyBufferedImage image = scanMapSeq.getValue().getImage( 0 , 0, 0 );
					image.setData((int)x_stage[0]/1000,(int)y_stage[0]/1000,0,100);
				}
				else
				{
					  new AnnounceFrame("No sequence selected!");
					  return;
				}
			} catch (Exception e1) {
				  new AnnounceFrame("Marking on sequence failed!");
				  return;
			}
			
		}
		else if (((JButton)e.getSource()).getText().equals(gotoPostion.name)) {
			 
				try {
					xyStageLabel = core.getXYStageDevice();
					core.setXYPosition(xyStageLabel, posX.getValue()*1000.0, posY.getValue()*1000.0);
					// new AnnounceFrame("Goto position...!");
				} catch (Exception e1) {
					  new AnnounceFrame("Goto position failed!");
					  return;
				}
		}
		else if (((JButton)e.getSource()).getText().equals(getPos.name)) {
			   xyStageLabel = core.getXYStageDevice();
				try {
					double[] x_stage = {0.0};
					double[] y_stage = {0.0};
					core.getXYPosition(xyStageLabel, x_stage, y_stage);
					posX.setValue(x_stage[0]/1000.0);
					posY.setValue(y_stage[0]/1000.0);
				} catch (Exception e1) {
					  new AnnounceFrame("Get position failed!");
					  return;
				}
		}
		else if (((JButton)e.getSource()).getText().equals(generatePath.name)) {
			
			System.out.println("Generate Path ...");
			
			try {
				if(scanMapSeq != null)
				{
					if(stepSize.getValue()<=0.0)
					{
						  new AnnounceFrame("Step size error!");
						  return;
					}
					
					ArrayList<ROI2D> rois;	
					PrintWriter pw ;
					try
					{
						rois= scanMapSeq.getValue().getROI2Ds();
					}
					catch (Exception e1)
					{
						new AnnounceFrame("Please add at least one 2d roi in the scan map sequence!");
						return;	
					}

					if(generateFilePath.getValue() != null)
					{
						pw = new PrintWriter(new FileWriter(generateFilePath.getValue()));
					}
					else
					{
						  new AnnounceFrame("Please select the 'Save Path'!");
						  return;
					}
				
					for(int i=0;i<rois.size();i++) 
					{
						ROI2D roi = rois.get(i);
						double x0 = roi.getBounds().getMinX();
						double y0 = roi.getBounds().getMinY();
						double x1 = roi.getBounds().getMaxX();
						double y1 = roi.getBounds().getMaxY();
						//for(double a=x0;a<=x1;a+=stepSize.getValue())

						pw.printf("(newSequence=%s-%d)\n",roi.getName(),i);
						pw.printf("(location=%d,%d)\n",(int)x0,(int)y0);
						pw.printf("(width=%d)\n",(int)((double)(x1-x0)/stepSize.getValue()-0.5));	
						pw.printf("(height=%d)\n",(int)((double)(y1-y0)/stepSize.getValue()-0.5));	
						pw.printf("(G90)\n");		
						pw.printf("M108 P%f Q%d\n",stepSize.getValue(),0);
						
						for(double b=y0;b<=y1;b+=stepSize.getValue())	
						{
							pw.printf("G00 X%f Y%f\n",x0,b);
							pw.printf("G01 X%f Y%f\n",x1,b);
						}
						pw.printf("G00 X0 Y0\n");
						pw.close();	
						
						pathFile.setValue(generateFilePath.getValue()); //set the path as the default value.
						new AnnounceFrame("Generated successfully!");
					}

				}
			} catch (Exception e1) {
				  new AnnounceFrame("Error when generate path file!");
				  return;
			}
			
		}
	}
	
}