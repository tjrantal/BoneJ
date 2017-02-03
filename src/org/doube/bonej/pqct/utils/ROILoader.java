
package org.doube.bonej.pqct.utils;

import ij.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.measure.*;
import ij.gui.*;	
import ij.process.*;
import ij.io.*;
import ij.text.TextPanel;
import java.util.*;
import java.awt.*;
import java.awt.event.*;	
import java.text.*;

/*Choosing and saving a file*/
import javax.swing.SwingUtilities;
import javax.swing.JFileChooser;
import java.util.prefs.Preferences;		/*Saving the file save path -> no need to re-browse...*/
import java.io.*;




public class ROILoader implements PlugIn {

	/**Implement the PlugIn interface
		Give the file path and name with ROI coordinates in Macro options.
		run("ROI Loader", "path/to/file/fileName.txt");
		This plug-in reads both endo- and pericortical
		coordiantes from a comma-separated text-file with four columns (ex, ey, px,py)
	*/
    public void run(String arg) {
		String fileIn = Macro.getOptions();
		//IJ.log("ROILoader "+fileIn);
		float[][] endoRoiCoordinates;
		float[][] periRoiCoordinates;
		try{
			//IJ.log("BufferedReader");
			BufferedReader br = new BufferedReader(new FileReader(fileIn));
			String line = br.readLine();	//Read the header line
			ArrayList<String[]> dataLines = new ArrayList<String[]>();
			line = br.readLine();
			while (line != null){
				dataLines.add(line.split(","));
				line = br.readLine();
			}
			//IJ.log("Got "+dataLines.size()+" lines of data");
			endoRoiCoordinates = new float[2][dataLines.size()];
			periRoiCoordinates = new float[2][dataLines.size()];
			for (int i =0;i<dataLines.size();++i){
				endoRoiCoordinates[0][i] = Float.parseFloat(dataLines.get(i)[0]);	//X-coordinate
				endoRoiCoordinates[1][i] = Float.parseFloat(dataLines.get(i)[1]);	//Y-coordinate
				periRoiCoordinates[0][i] = Float.parseFloat(dataLines.get(i)[2]);	//X-coordinate
				periRoiCoordinates[1][i] = Float.parseFloat(dataLines.get(i)[3]);	//Y-coordinate
				//IJ.log("X "+endoRoiCoordinates[i][0]+" Y "+endoRoiCoordinates[i][1]);
			}
		}catch (Exception err){
			System.out.println(err);
			return;
		}

		PolygonRoi endoRoi = new PolygonRoi(endoRoiCoordinates[0],endoRoiCoordinates[1],endoRoiCoordinates[0].length,Roi.POLYGON);//Roi.POLYLINE);
		PolygonRoi periRoi = new PolygonRoi(periRoiCoordinates[0],periRoiCoordinates[1],periRoiCoordinates[0].length,Roi.POLYGON);//Roi.POLYLINE);
		
		RoiManager rm;
		rm = RoiManager.getInstance2();
		if (rm != null){
			rm.reset();	//Remove any pre-existing ROIs
		}else{
			rm = new RoiManager();
		}
		rm.addRoi(endoRoi);	//add endocortex
		rm.addRoi(periRoi);	//add pericortex

	}
}
