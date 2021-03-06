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

	ImageJ density distribution analysis plugin
    Copyright (C) 2011 Timo Rantalainen
*/

package org.doube.bonej.pqct;

import ij.*;
import ij.text.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;						//Calibration
import java.util.*;							//Vector
import java.io.*;							//File IO for reading .TYP files
import ij.plugin.PlugIn;
import org.doube.bonej.pqct.analysis.*;		//Analysis stuff..
import org.doube.bonej.pqct.selectroi.*;	//ROI selection..
import org.doube.bonej.pqct.io.*;			//image data
import org.doube.bonej.pqct.utils.*;		//Writing results and creating visual result image
import org.doube.util.UsageReporter;

import java.awt.*;							//Image, component for debugging...
import ij.plugin.filter.Info;
import ij.io.*;
import java.util.concurrent.ExecutionException;

public class Distribution_Analysis implements PlugIn {


	String resultString;
	String imageInfo;
	double resolution;
	boolean alphaOn;

	public void run(String arg) {
	   	//IJ.log("Start plug-in");
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)
			return;
		if (imp.getType() != ImagePlus.GRAY16){
			IJ.error("Distribution analysis expects 16-bit greyscale data");
			return;
		}
		//IJ.log("Got 16 bit");
		/*Set sector widths and division numbers*/
		int[] sectorsAndDivisions = {10,3,10,10}; /*Distribution analysis sectorWidth, Distribution analysis sectors, Concentric distribution analysis sectorWidth, Concentric distribution analysis sectors*/
		int[] filterSizes = {3,7};		//first is used for bone analysis filtering and second for soft tissue analysis filtering. ?x? median filter.

		//A ROI appears on the image every now and then, haven't figured out why -> remove any unwanted rois prior to soft-tissue analysis
		byte removeROIs = 0;
		if (imp.getRoi() == null){
			removeROIs = 1;
		}
		//IJ.log("Get Image info");
		imageInfo = new Info().getImageInfo(imp,imp.getChannelProcessor());
		/*Check image calibration*/
		//IJ.log("Try to get calibration");
		Calibration cal = imp.getCalibration();
		double[] calibrationCoefficients = {0,1};
		if (getInfoProperty(imageInfo,"Stratec File") == null){
			//IJ.log("Try to access calibration");
			try{
				if (cal != null && cal.getCoefficients() != null){
				   	//IJ.log("Trying to get coefficients");
					calibrationCoefficients = cal.getCoefficients();
				   	//IJ.log("Got coefficients c0 "+calibrationCoefficients[0]+" c1 "+calibrationCoefficients[0] );
				}
			}catch (Exception err) {
			 	//IJ.log("Didn't have calibration");
			}
			//IJ.log("Got past calib");
		} else {
			calibrationCoefficients = new double[2];
			/*Read calibration from TYP file database*/
			String typFileName = getInfoProperty(imageInfo,"Device");
			try {
				InputStream ir = this.getClass().getClassLoader().getResourceAsStream("org/doube/bonej/pqct/typ/"+typFileName);
				byte[] typFileData = new byte[ir.available()];
				ir.read(typFileData);
				ir.close();
				String typFiledDataString = new String(typFileData,"ISO-8859-1");
				//break the typFileDataString into lines
				StringTokenizer st = new StringTokenizer(typFiledDataString, "\n");
				Vector<String> typFileLines = new Vector<String>();
				while(st.hasMoreTokens()){
					typFileLines.add(st.nextToken());
				}
				//Search for XSlope and XInter
				String[] searchFor = {"XInter","XSlope"};
				for (int i = 0;i<searchFor.length;++i){
					int index = 0;
					String temp = typFileLines.get(index);
					while (temp.indexOf(searchFor[i]) < 0 && index < typFileLines.size()){
						++index;
						temp = typFileLines.get(index);
					}
					if (temp.indexOf(searchFor[i]) >= 0){	//Found line
						StringTokenizer st2 = new StringTokenizer(temp, "=");
						Vector<String> typFileLineTokens = new Vector<String>();
						while(st2.hasMoreTokens()){
							typFileLineTokens.add(st2.nextToken().trim());
						}
						calibrationCoefficients[i] = Double.valueOf(typFileLineTokens.get(1));
					} else {
						calibrationCoefficients[i] = (double) i*1000.0;
					}
				}
				calibrationCoefficients[1] /= 1000.0;		//1.495
			} catch (Exception err){System.err.println("Error: "+err.getMessage());}
		}
		//IJ.log("tRYING TO get resol");
		resolution = cal.pixelWidth;
		//IJ.log("Got resolution "+resolution);
		if (getInfoProperty(imageInfo,"Pixel Spacing")!= null){
			String temp = getInfoProperty(imageInfo,"Pixel Spacing");
			if (temp.indexOf("\\")!=-1){
				temp = temp.substring(0,temp.indexOf("\\"));
			}
			resolution = Double.valueOf(temp);
		}
		//Get parameters for scaling the image and for thresholding
		GenericDialog dialog = new GenericDialog("Analysis parameters");
		String[] topLabels = new String[6];
		boolean[] defaultTopValues = new boolean[6];
		topLabels[0] = "Flip_horizontal";
		defaultTopValues[0] = false;
		topLabels[1] = "Flip_vertical";
		defaultTopValues[1] = false;
		topLabels[2] = "No_filtering";
		defaultTopValues[2] = false;
		topLabels[3] = "Measurement_tube";
		defaultTopValues[3] = false;
		topLabels[4] = "Use_Lasso";		//Added 2017/02/03, select 'lassoing' muscles to include intermuscular fat into muscle area
		defaultTopValues[4] = false;
		topLabels[5] = "Use_Thresholdless";	//Added 2017/02/03, use thresholdless analysis for density distribution analysis
		defaultTopValues[5] = false;
		dialog.addCheckboxGroup(1, 6, topLabels, defaultTopValues);

		dialog.addNumericField("Air_threshold", -40, 4, 8, null);	//Anything above this is fat or more dense
		dialog.addNumericField("Fat threshold", 40, 4, 8, null);		//Anything between this and air threshold is fat
		dialog.addNumericField("Muscle_threshold", 40, 4, 8, null);		//Anything above this is muscle or more dense
		dialog.addNumericField("Edge_divisions", 45, 4, 8, null);		//Used with livewire to include intermuscular fat
		dialog.addNumericField("Marrow_threshold", 80, 4, 8, null);		//Anything above this is bone
		dialog.addNumericField("Soft_tissue_threshold", 200.0, 4, 8, null);		//Anything  between this and muscle threshold is muscle
		dialog.addNumericField("Rotation_threshold", 200.0, 4, 8, null);
		dialog.addNumericField("Area threshold", 550.0, 4, 8, null); 	//550.0
		dialog.addNumericField("BMD threshold", 690.0, 4, 8, null);		//690.0


		dialog.addNumericField("Scaling_coefficient (slope)", calibrationCoefficients[1], 4, 8, null);
		dialog.addNumericField("Scaling_constant (intercept)",calibrationCoefficients[0], 4, 8, null);

		/*
		//Debugging
		dialog.addNumericField("Scaling_coefficient (slope)", 0.821, 4, 8, null);
		dialog.addNumericField("Scaling_constant (intercept)",-856.036, 4, 8, null);
		*/
		//Get ROI selection
		String[] choiceLabels = {"Bigger","Smaller","Left","Right","Top","Bottom","Central","Peripheral","SecondLargest","TwoLargestLeft","TwoLargestRight"
								,"FirstFromLeft","SecondFromLeft","ThirdFromLeft","FourthFromLeft","FifthFromLeft"
								,"FirstFromTop","SecondFromTop","ThirdFromTop","FourthFromTop","FifthFromTop"};
		dialog.addChoice("Roi_selection", choiceLabels, choiceLabels[0]);
		dialog.addChoice("Soft_Tissue_Roi_selection", choiceLabels, choiceLabels[0]);
		String[] rotationLabels = {"According_to_Imax/Imin","Furthest_point","All_Bones_Imax/Imin","Not_selected_to_right","Selected_to_right"};
		dialog.addChoice("Rotation_selection", rotationLabels, rotationLabels[0]); //"According_to_Imax/Imin"

		String[] middleLabels = new String[10];
		boolean[] middleDefaults = new boolean[10];
		middleLabels[0] = "Analyse_cortical_results";
		middleDefaults[0] = false;//true;
		middleLabels[1] = "Analyse_mass_distribution";
		middleDefaults[1] = false;
		middleLabels[2] = "Analyse_concentric_density_distribution";
		middleDefaults[2] = false;
		middleLabels[3] = "Analyse_density_distribution";
		middleDefaults[3] = false;//true;
		middleLabels[4] = "Analyse_soft_tissues";
		middleDefaults[4] = true;//false;
		middleLabels[5] = "Prevent_peeling_PVE_pixels";
		middleDefaults[5] = false;
		middleLabels[6] = "Allow_cleaving";
		middleDefaults[6] = false;
		middleLabels[7] = "Suppress_result_image";
		middleDefaults[7] = false;
		middleLabels[8] = "Limit_ROI_search_to_manually_selected";
		middleDefaults[8] = false;
		middleLabels[9] = "Set_distribution_results_rotation_manually";
		middleDefaults[9] = false;
		dialog.addCheckboxGroup(4, 3, middleLabels, middleDefaults);

		dialog.addNumericField("Manual_rotation_[+-_180_deg]", 0.0, 4, 8, null);

		String[] bottomLabels = new String[8];
		boolean[] bottomDefaults = new boolean[8];
		bottomLabels[0] = "Guess_flip";
		bottomDefaults[0] = false;
		bottomLabels[1] = "Guess_right";
		bottomDefaults[1] = false;
		bottomLabels[2] = "Guess_larger";
		bottomDefaults[2] = false;
		bottomLabels[3] = "Stacked_bones";
		bottomDefaults[3] = false;
		bottomLabels[4] = "Guess_stacked";
		bottomDefaults[4] = false;
		bottomLabels[5] = "Invert_flip_guess";
		bottomDefaults[5] = false;
		bottomLabels[6] = "Flip_distribution_results";
		bottomDefaults[6] = false;
		bottomLabels[7] = "Save_visual_result_image_on_disk";
		bottomDefaults[7] = false;
		dialog.addCheckboxGroup(2, 5, bottomLabels, bottomDefaults);

		dialog.addStringField("Image_save_path",Prefs.getDefaultDirectory(),40);
		dialog.addHelp("http://bonej.org/densitydistribution");
		dialog.showDialog();
				//IJ.log("Dialog done");
		if (dialog.wasOKed()){ //Stop in case of cancel..
			for (int i = 0; i<defaultTopValues.length;++i){
				defaultTopValues[i] = dialog.getNextBoolean();
			}
			double[] thresholdsAndScaling = new double[11];
			for (int i = 0; i<thresholdsAndScaling.length;++i){
				thresholdsAndScaling[i]		= dialog.getNextNumber();
			}
			String[] alignmentStrings = new String[3];
			for (int i = 0; i<alignmentStrings.length;++i){
				alignmentStrings[i]		= dialog.getNextChoice();
			}
			for (int i = 0; i<middleDefaults.length;++i){
				middleDefaults[i] = dialog.getNextBoolean();
			}
			double manualAlfa			= dialog.getNextNumber()*Math.PI/180.0;
			for (int i = 0; i<bottomDefaults.length;++i){
				bottomDefaults[i] = dialog.getNextBoolean();
			}
			String imageSavePath 		= dialog.getNextString();
			ScaledImageData scaledImageData;

			String imageName;
			if (getInfoProperty(imageInfo,"File Name")!= null){
				imageName = getInfoProperty(imageInfo,"File Name");
			}else{
				if(imp.getImageStackSize() == 1){
					imageName = imp.getTitle();
					imageInfo+="File Name:"+imageName+"\n";
				}else{
					imageName = imageInfo.substring(0,imageInfo.indexOf("\n"));
					imageInfo+="File Name:"+imageName+"\n";
				}
			}

			/*Look study special, images acquired prior to 2007 need to be flipped horizontally
			String checkDate = getInfoProperty(imageInfo,"Acquisition Date");
			if (checkDate.indexOf("2005") >-1 || checkDate.indexOf("2006") >-1){
				flipHorizontal = true;
			}
			*/
			//IJ.log("Get pixels");
			short[] tempPointer = (short[]) imp.getProcessor().getPixels();
			int[] signedShort = new int[tempPointer.length];
			//IJ.log("Try calib");
			if (imp.getOriginalFileInfo().fileType == ij.io.FileInfo.GRAY16_SIGNED || cal.isSigned16Bit()){
			   	//IJ.log("Float");
				float[] floatPointer = (float[]) imp.getProcessor().toFloat(1,null).getPixels();
				for (int i=0;i<tempPointer.length;++i){signedShort[i] = (int) (floatPointer[i] - Math.pow(2.0,15.0));}
			} else {
				/*
				Apply the original calibration of the image prior to applying the calibration got from the user
				-> enables using ImageJ for figuring out the calibration without too much fuss.
				*/
				//IJ.log("CalCoeffs");
				try{
					double[] origCalCoeffs = imp.getOriginalFileInfo().coefficients;
					if (origCalCoeffs == null){origCalCoeffs = cal.getCoefficients();}
					float[] floatPointer = (float[]) imp.getProcessor().toFloat(1,null).getPixels();
					for (int i=0;i<tempPointer.length;++i){signedShort[i] = (int) (floatPointer[i]*origCalCoeffs[1]+origCalCoeffs[0]);}
				}catch (Exception err){
					//IJ.log("No scaling");
				   	for (int i=0;i<tempPointer.length;++i){signedShort[i] = (int) tempPointer[i];}
				}
			}
			//IJ.log("Got calib");
			ImageAndAnalysisDetails imageAndAnalysisDetails = new ImageAndAnalysisDetails(	defaultTopValues,
																							thresholdsAndScaling,
																							alignmentStrings,choiceLabels,rotationLabels,
																							middleDefaults,
																							manualAlfa,
																							bottomDefaults,
																							sectorsAndDivisions,
																							filterSizes);
			scaledImageData = new ScaledImageData(signedShort, imp.getWidth(), imp.getHeight(),resolution, imageAndAnalysisDetails.scalingFactor, imageAndAnalysisDetails.constant,3,imageAndAnalysisDetails.flipHorizontal,imageAndAnalysisDetails.flipVertical,imageAndAnalysisDetails.noFiltering);	//Scale and 3x3 median filter the data
			RoiSelector roi = null;
			RoiSelector roiRoi = null; 	//Added 2017/02/03, use for thresholdless analysis
			RoiSelector softRoi = null;
			
			try{
				if(imageAndAnalysisDetails.cOn || imageAndAnalysisDetails.mOn || imageAndAnalysisDetails.conOn || imageAndAnalysisDetails.dOn){
					if (imageAndAnalysisDetails.thresholdless){
						//Calculate a thresholdless roi
						roiRoi = new ThresholdlessROI(scaledImageData, imageAndAnalysisDetails,imp,imageAndAnalysisDetails.boneThreshold,true);
					}
					
					roi = new SelectROI(scaledImageData, imageAndAnalysisDetails,imp,imageAndAnalysisDetails.boneThreshold,true);
					
				}
				
				if(imageAndAnalysisDetails.stOn){
					if (removeROIs ==1){
						imp.setRoi(null,false);	//Remove unwanted ROIS
					}
					if (imageAndAnalysisDetails.lasso){
						softRoi = new SelectSoftROILasso(scaledImageData, imageAndAnalysisDetails,imp,imageAndAnalysisDetails.boneThreshold,true);
					}else{
						softRoi = new SelectSoftROI(scaledImageData, imageAndAnalysisDetails,imp,imageAndAnalysisDetails.boneThreshold,true);
					}
					if (roi == null){
						roi = softRoi;
					}
				}
			}catch (ExecutionException err){
				IJ.log("Caught sieve error "+err.toString());
				return;
			}

			if (roi != null){	/*An analysis was conducted*/
				alphaOn = false;
				DetermineAlfa determineAlfa = null;
				if (imageAndAnalysisDetails.cOn || imageAndAnalysisDetails.mOn || imageAndAnalysisDetails.conOn || imageAndAnalysisDetails.dOn){
					determineAlfa = new DetermineAlfa((SelectROI) roi,imageAndAnalysisDetails);
					alphaOn = true;
				}

				imageAndAnalysisDetails.flipDistribution = roi.details.flipDistribution;
				imageAndAnalysisDetails.stacked = roi.details.stacked;

				TextPanel textPanel = IJ.getTextPanel();
				if (textPanel == null) {textPanel = new TextPanel();}
				ResultsWriter resultsWriter = new ResultsWriter(imageInfo,alphaOn);

				if (textPanel.getLineCount() == 0){resultsWriter.writeHeader(textPanel,imageAndAnalysisDetails);}

				String results = "";
				results = resultsWriter.printResults(results,imageAndAnalysisDetails, imp);
				if (determineAlfa != null){
					results = resultsWriter.printAlfa(results,determineAlfa);
				}

				ImagePlus resultImage = null;
				boolean makeImage = true;
				if(imageAndAnalysisDetails.suppressImages && !imageAndAnalysisDetails.saveImageOnDisk && roi != null){
					makeImage = false;
				}else{
					//System.out.println("Trying to create image "+roi.scaledImage.length+" "+roi.width+" "+roi.height);
					resultImage = ResultsImage.getRGBResultImage(roi.scaledImage,roi.width,roi.height, imageSavePath);
					//System.out.println("Returned image "+resultImage.getWidth()+" height "+resultImage.getHeight());
					resultImage.setTitle(imp.getTitle()+"-result");
				}
				//IJ.log("ST.");
				if(imageAndAnalysisDetails.stOn){
					SoftTissueAnalysis softTissueAnalysis = new SoftTissueAnalysis((SelectSoftROI) softRoi);
					results = resultsWriter.printSoftTissueResults(results,softTissueAnalysis);
					if(makeImage && resultImage != null){
						//IJ.log("Adding soft tissues");
						resultImage = ResultsImage.addSoftTissueSieve(resultImage,softRoi.softSieve);
						//System.out.println("ST image "+resultImage.getWidth()+" height "+resultImage.getHeight());
					}
				}
				//IJ.log("cON.");
				if (imageAndAnalysisDetails.cOn){
					//IJ.log("Adding cortical");
					CorticalAnalysis cortAnalysis =new CorticalAnalysis((SelectROI) roi);
					results = resultsWriter.printCorticalResults(results,cortAnalysis);
					if(makeImage && resultImage != null){
						resultImage = ResultsImage.addBoneSieve(resultImage,roi.sieve,roi.scaledImage,roi.details.marrowThreshold,cortAnalysis.cortexSieve);
						//System.out.println("cON image "+resultImage.getWidth()+" height "+resultImage.getHeight());
					}

				}
				//IJ.log("mON.");
				if (imageAndAnalysisDetails.mOn){
					//IJ.log("Adding mass dist");
					MassDistribution massDistribution =new MassDistribution((SelectROI) roi,imageAndAnalysisDetails,determineAlfa);
					results = resultsWriter.printMassDistributionResults(results,massDistribution,imageAndAnalysisDetails);
					//System.out.println("mON image "+resultImage.getWidth()+" height "+resultImage.getHeight());
				}
				//IJ.log("conON.");
				if (imageAndAnalysisDetails.conOn){
					//IJ.log("Adding concentric");
					ConcentricRingAnalysis concentricRingAnalysis;
					if (imageAndAnalysisDetails.thresholdless){
						concentricRingAnalysis =new ConcentricRingAnalysis((SelectROI) roiRoi,imageAndAnalysisDetails,determineAlfa);
					}else{
						concentricRingAnalysis =new ConcentricRingAnalysis((SelectROI) roi,imageAndAnalysisDetails,determineAlfa);
					}
					
					results = resultsWriter.printConcentricRingResults(results,concentricRingAnalysis,imageAndAnalysisDetails);
					if(!imageAndAnalysisDetails.dOn && makeImage && resultImage != null){
						resultImage = ResultsImage.addPeriRadii(resultImage,concentricRingAnalysis.boneCenter, determineAlfa.pindColor,concentricRingAnalysis.Ru,concentricRingAnalysis.Theta);
						resultImage = ResultsImage.addMarrowCenter(resultImage,determineAlfa.alfa/Math.PI*180.0,concentricRingAnalysis.boneCenter);
						//System.out.println("conON image "+resultImage.getWidth()+" height "+resultImage.getHeight());
					}
				}

				//IJ.log("dON.");
				if (imageAndAnalysisDetails.dOn){
					//IJ.log("Adding distribution");
					double tempThresh = imageAndAnalysisDetails.BMDthreshold;
					DistributionAnalysis distributionAnalysis;
					if (imageAndAnalysisDetails.thresholdless){
						imageAndAnalysisDetails.BMDthreshold = 0d; //Lower the threshold to include all bone within cortex area
						distributionAnalysis = new DistributionAnalysis((SelectROI) roiRoi,imageAndAnalysisDetails,determineAlfa);
						imageAndAnalysisDetails.BMDthreshold = tempThresh; //Return the BMD threshold to its original value
					}else{
						distributionAnalysis = new DistributionAnalysis((SelectROI) roi,imageAndAnalysisDetails,determineAlfa);
					}
					
					results = resultsWriter.printDistributionResults(results,distributionAnalysis,imageAndAnalysisDetails);
					if (makeImage && resultImage != null){
						resultImage = ResultsImage.addRadii(resultImage,determineAlfa.alfa/Math.PI*180.0,distributionAnalysis.marrowCenter, determineAlfa.pindColor,distributionAnalysis.R,distributionAnalysis.R2,distributionAnalysis.Theta);
						resultImage = ResultsImage.addMarrowCenter(resultImage,determineAlfa.alfa/Math.PI*180.0,distributionAnalysis.marrowCenter);
						//System.out.println("dON image "+resultImage.getWidth()+" height "+resultImage.getHeight());
					}
				}

				//IJ.log("rotate.");
				//IJ.log("Ready to rotate image "+(determineAlfa.alfa/Math.PI*180.0));
				if ((imageAndAnalysisDetails.dOn || imageAndAnalysisDetails.conOn) && makeImage && resultImage != null){
					//IJ.log("Adding rotation");
					resultImage = ResultsImage.addRotate(resultImage,determineAlfa.alfa/Math.PI*180.0);
					//System.out.println("rotate image "+resultImage.getWidth()+" height "+resultImage.getHeight()+" rotated "+(determineAlfa.alfa/Math.PI*180.0));
				}

				//IJ.log("scale. "+resultImage.getWidth()+" height "+resultImage.getHeight());
				if (!imageAndAnalysisDetails.suppressImages && resultImage!= null){
					resultImage = ResultsImage.addScale(resultImage,roi.pixelSpacing);	//Add scale after rotating
					//IJ.log("Image done");
					//System.out.println("Scale done "+resultImage.getWidth()+" height "+resultImage.getHeight());
					resultImage.show();
				}
				//IJ.log("saveImage.");
				if (imageAndAnalysisDetails.saveImageOnDisk && resultImage!= null){
					if (imageAndAnalysisDetails.suppressImages){
						resultImage = ResultsImage.addScale(resultImage,roi.pixelSpacing);	//Add scale after rotating
						//System.out.println("Scale surpress done "+resultImage.getWidth()+" height "+resultImage.getHeight());
					}
					//System.out.println("Creating fileSaver");
					FileSaver fSaver = new FileSaver(resultImage);
					//System.out.println("Saving");
					fSaver.saveAsPng(imageSavePath+imageName+".png");
					//System.out.println("Saving done");
				}
				//IJ.log("saveSuccessful.");
				textPanel.appendLine(results);
				textPanel.updateDisplay();
			}else{
				IJ.log("No analysis was selected.");
			}
		}
		UsageReporter.reportEvent(this).send();
	}

	public static String getInfoProperty(String properties,String propertyToGet){
		String toTokenize = properties;
		StringTokenizer st = new StringTokenizer(toTokenize,"\n");
		String currentToken = null;
		while (st.hasMoreTokens() ) {
			currentToken = st.nextToken();
			if (currentToken.indexOf(propertyToGet) != -1){break;}
		}
		if (currentToken.indexOf(propertyToGet) != -1){
			StringTokenizer st2 = new StringTokenizer(currentToken,":");
			String token2 = null;
			while (st2.hasMoreTokens()){
				token2 = st2.nextToken();
			}
			return token2.trim();
		}
		return null;
	}

}
