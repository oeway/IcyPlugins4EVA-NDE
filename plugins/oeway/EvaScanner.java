package plugins.oeway;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopeCore;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopePluginAcquisition;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopeSequence;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.Tools.ImageGetter;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.Tools.StageMover;
import icy.gui.frame.progress.AnnounceFrame;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.sequence.Sequence;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;

import javax.swing.JButton;

import plugins.adufour.ezplug.*;
 
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
    Sequence currentSequence;
	EzButton 					markPos;
	EzButton 					generatePath;
	EzVarDouble					stepSize;
	EzVarSequence 				scanMapSeq;
	
	EzVarDouble					scanSpeed;
	EzVarFile					pathFile;
	EzVarText					note;
	EzVarFolder					targetFolder;


	
	// some other data
	boolean						stopFlag;
	
	@Override
	protected void initialize()
	{
		// 1) variables must be initialized
		scanMapSeq = new EzVarSequence("Scan Map Sequence");
		stepSize = new EzVarDouble("Step Size");
		markPos = new EzButton("Mark Position", this);
		generatePath = new EzButton("Generate Path", this);		
		
		scanSpeed = new EzVarDouble("Scan Speed");
		pathFile = new EzVarFile("Path File", null);
		
		note = new EzVarText("Scan Note", new String[] { "Test" }, 0, true);
		
		targetFolder = new EzVarFolder("Target Folder", null);
		
		// 2) and added to the interface in the desired order
		
		// let's group other variables per type
		
		EzGroup groupScanMap = new EzGroup("Scan Map", scanMapSeq, stepSize,markPos,generatePath);
		super.addEzComponent(groupScanMap);	
		
		
		EzGroup groupSettings = new EzGroup("Settings", pathFile, scanSpeed,targetFolder,note);
		super.addEzComponent(groupSettings);
		
		core = MicroscopeCore.getCore();
		
	}
	protected void createSequence(String name)
	{
		
        IcyBufferedImage capturedImage;
        if (core.isSequenceRunning())
            capturedImage = ImageGetter.getImageFromLive(core);
        else
            capturedImage = ImageGetter.snapImage(core);
        if (capturedImage == null)
        {
            new AnnounceFrame("No image was captured");
            return;
        }
        MicroscopeSequence s = new MicroscopeSequence(capturedImage);
        Calendar calendar = Calendar.getInstance();
        Icy.getMainInterface().addSequence(s);
        s.setName(name + "__" + calendar.get(Calendar.MONTH) + "_" + calendar.get(Calendar.DAY_OF_MONTH) + "_"
                + calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.HOUR_OF_DAY) + "_"
                + calendar.get(Calendar.MINUTE) + "_" + calendar.get(Calendar.SECOND));
		
	}
	@Override
	protected void execute()
	{
		// main plugin code goes here, and runs in a separate thread
		if(pathFile.getValue() == null){
			super.getUI().setProgressBarMessage("Please select a path file!");
			return;
		}
		
//		if(targetFolder.getValue() == null){
//			super.getUI().setProgressBarMessage("Please select a target folder to store data!");
//			return;
//		}
		
		
		System.out.println(scanSpeed.name + " = " + scanSpeed.getValue());
		System.out.println(pathFile.name + " = " + pathFile.getValue());
		System.out.println(targetFolder.name + " = " + targetFolder.getValue());
		System.out.println(note.name + " = " + note.getValue());


		try{
		  // Open the file that is the first 
		  // command line parameter
		  FileInputStream fstream = new FileInputStream("c:/scanMap.txt");
		  // Get the object of DataInputStream
		  DataInputStream in = new DataInputStream(fstream);
		  BufferedReader br = new BufferedReader(new InputStreamReader(in));
		  String strLine;
		  //Read File Line By Line
		  HashMap<String , String> settings = new HashMap<String , String>();   
		  while ((strLine = br.readLine()) != null) {
			  	// Print the content on the console
			  	strLine = strLine.trim();
			  	System.out.println (strLine);
			  	if(strLine.startsWith("(") && strLine.endsWith(")") && strLine.contains("=")){  //comment
			  		strLine = strLine.replace("(", ""); 
			  		strLine = strLine.replace(")", "");
			  		String tmp[] = strLine.split("=");
			  		settings.put(tmp[0],tmp[1]);
			  		
			  		if(tmp[0] == "newSequence" ){
			  			createSequence(tmp[1]);
			  		}
			  		else if(tmp[0] == "width"){
			  			
			  		}
			  		else if(tmp[0] == "height"){
			  			
			  		}
			  		else{
			  			
			  		}
			  	}
			  	else if (strLine.startsWith("G01")){
			  		
			  	}
			  	else{
			  		
			  	}
			  }
		  //Close the input stream
		  in.close();
		}
		catch (Exception e){//Catch exception if any
			
			  System.err.println("Error: " + e.getMessage());
			  return;
		}
		stopFlag = false;
		super.getUI().setProgressBarMessage("Waiting...");
		
		int cpt = 0;
		while (!stopFlag)
		{
			cpt++;
			if (cpt % 10 == 0) super.getUI().setProgressBarValue((cpt % 5000000) / 5000000.0);
			Thread.yield();
			
			
			
		}
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
		
		if (((JButton)e.getSource()).getText() == markPos.name) {
			
			System.out.println("Mark Pos");
			
		}
		else if (((JButton)e.getSource()).getText() == generatePath.name) {
			
			System.out.println("Generate Path ...");
			
		}
	}
	
}