/*
	This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

	N.B.  the above text was copied from http://www.gnu.org/licenses/gpl.html
	unmodified. I have not attached a copy of the GNU license to the source...

    Copyright (C) 2011 Timo Rantalainen
*/

package org.doube.bonej.pqct.selectroi;
import java.util.*;	//Vector, Collections
import java.lang.Math; //atan2
import java.awt.*;			//Polygon, Rectangle
import org.doube.bonej.pqct.io.*;	//image data
import ij.*;		//ImagePlus
import ij.gui.*;	//ImagePlus ROI
import ij.text.*; 	//Debugging ...
import ij.process.*;	//Debugging
import ij.plugin.frame.RoiManager;
import java.util.concurrent.ExecutionException;
import org.doube.bonej.pqct.utils.VisualiseSieve;

@SuppressWarnings(value ={"serial","unchecked"}) //Unchecked for obtaining Vector<Object> as a returnvalue

public class ThresholdlessROI extends SelectROI{
	//ImageJ constructor
	public ThresholdlessROI(ScaledImageData dataIn,ImageAndAnalysisDetails detailsIn, ImagePlus imp,double boneThreshold,boolean setRoi) throws ExecutionException{
		super(dataIn,detailsIn, imp,boneThreshold,setRoi);
		//Select ROI
	}
	
	@Override
	public void execute() throws ExecutionException{
		//IJ.log("Calling ThresholdlessROI execute");
		//Get the endo an peri ROIs from roiManager
		RoiManager rm = RoiManager.getInstance2();	//Get a reference to the roi manager
		Roi endoRoi = rm.getRoi(0);		//Pre-load these manually with ROILoader
		Roi periRoi = rm.getRoi(1);		//Pre-load these manually with ROILoader
		Roi ijROI;// = imp.getRoi();
		double[] tempScaledImage = Arrays.copyOf(scaledImage,scaledImage.length);//(double[]) scaledImage.clone();
		
		/*Check whether pixel is within ROI, mark with bone threshold*/
		for (int j = 0;j< height;j++){
			for (int i = 0; i < width;i++){
				if (!periRoi.contains(i,j) | endoRoi.contains(i,j)){
					tempScaledImage[i+j*width] = minimum;
				}
			}
		}
		/*Check whether a polygon can be acquired and include polygon points too*/
		Polygon polygon = periRoi.getPolygon();
		if (polygon != null){
			for (int j = 0;j< polygon.npoints;j++){
				tempScaledImage[polygon.xpoints[j]+polygon.ypoints[j]*width] = scaledImage[polygon.xpoints[j]+polygon.ypoints[j]*width];
			}
		}
		polygon = endoRoi.getPolygon();
		if (polygon != null){
			for (int j = 0;j< polygon.npoints;j++){
				tempScaledImage[polygon.xpoints[j]+polygon.ypoints[j]*width] = scaledImage[polygon.xpoints[j]+polygon.ypoints[j]*width];
			}
		}
		
		try{
			Vector<Object> boneMasks = getSieve(tempScaledImage,0d,"Bigger",false,false,false,false);
			sieve							= (byte[]) boneMasks.get(0);
			result	 						= (byte[]) boneMasks.get(1);
			Vector<DetectedEdge> boneEdges	= (Vector<DetectedEdge>) boneMasks.get(2);
			selection						= (Integer)	 boneMasks.get(3);
			/*Add the roi to the image*/
			if (setRoi){
				int[] xcoordinates = new int[boneEdges.get(selection).iit.size()];
				int[] ycoordinates = new int[boneEdges.get(selection).iit.size()];
				for (int i = 0;i<boneEdges.get(selection).iit.size();++i){
					xcoordinates[i] = boneEdges.get(selection).iit.get(i);
					ycoordinates[i] = boneEdges.get(selection).jiit.get(i);
				}
				/*Flip the original image prior to adding the ROI, if scaled image is flipped*/
				if ((details.flipHorizontal || details.flipVertical) && imp.getRoi() != null){
					IJ.run(imp,"Select None","");	//Remove existing ROIs in order to flip the whole image...
				}
				if (details.flipHorizontal){imp.getProcessor().flipVertical(); imp.updateAndDraw();}
				if (details.flipVertical){imp.getProcessor().flipHorizontal(); imp.updateAndDraw();}
				ijROI = new PolygonRoi(xcoordinates,ycoordinates,boneEdges.get(selection).iit.size(),Roi.POLYGON);
				imp.setRoi(ijROI);
			}
			
			for (int j = 0;j< height;j++){
				for (int i = 0; i < width;i++){
					if (tempScaledImage[i+j*width]<0d && sieve[i+j*width] > 0){
						boneMarrowRoiI.add(i);
						boneMarrowRoiJ.add(j);
					}
					if (tempScaledImage[i+j*width]>=0d && sieve[i+j*width] > 0){
						cortexAreaRoiI.add(i);
						cortexAreaRoiJ.add(j);
					}
					if (tempScaledImage[i+j*width]>=0d && sieve[i+j*width] > 0){
						cortexROI[i+j*width] = scaledImage[i+j*width];				
						cortexRoiI.add(i);
						cortexRoiJ.add(j);
					} else {
						cortexROI[i+j*width] = minimum;
					}
				}
			}
			//VisualiseSieve.showImage(cortexROI,width,height);
			edges = boneEdges;
		}catch (ExecutionException err){
			throw err;
		}
	}	
}
