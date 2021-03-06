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

package org.doube.bonej.pqct.analysis;
import org.doube.bonej.pqct.selectroi.*;	//ROI selection..
import java.util.Arrays;

public class SoftTissueAnalysis{
	public double MuA;
	public double IntraMuFatA;
	public double TotalMuA;
	public double FatA;
	public double SubCutFatA;
	public double LimbA;
	public double MuD;
	public double IntraMuFatD;
	public double TotalMuD;
	public double FatD;
	public double SubCutFatD;
	public double SubCutFatDMedian;
	public double LimbD;
	public double FatPercentage;
	public double MeA;
	public double MeD;
	public double BoneA;
	public double BoneD;
	public double PeeledA;
	public double PeeledD;
	public SoftTissueAnalysis(SelectSoftROI roi){
		MuA			=0;
		FatA		=0;
		LimbA		=0;
		IntraMuFatA	=0;
		TotalMuA	=0;
		MuD			=0;
		FatD		=0;
		LimbD		=0;
		IntraMuFatD	=0;
		TotalMuD	=0;
		SubCutFatA	=0;
		SubCutFatD	=0;
		MeA			=0;
		MeD			=0;
		BoneA		=0;
		BoneD		=0;
		PeeledA		=0;
		PeeledD		=0;
		double weightedFatArea = 0;
		double weightedLimbArea = 0;
		for (int i =0;i<roi.width*roi.height;i++){
			if (roi.softSieve[i] >0){ //Bone & Marrow not excluded!!
				LimbA +=1;
				LimbD +=roi.softScaledImage[i];
				weightedLimbArea += roi.softScaledImage[i]+1000.0;
			}
			if (roi.softSieve[i] ==2 || roi.softSieve[i] ==4 || roi.softSieve[i] ==5){ //Fat
				FatA +=1;
				FatD +=roi.softScaledImage[i];
				weightedFatArea += roi.softScaledImage[i]+1000.0;
			}
			if (roi.softSieve[i] ==3){ //Muscle no IntraFat
				MuA +=1;
				MuD +=roi.softScaledImage[i];
				TotalMuA	+=1;
				TotalMuD	+=roi.softScaledImage[i];
			}
			if (roi.softSieve[i] ==4){ //IntraFat
				IntraMuFatA	+=1;
				IntraMuFatD	+=roi.softScaledImage[i];
				TotalMuA	+=1;
				TotalMuD	+=roi.softScaledImage[i];
				//weightedFatArea += roi.softScaledImage[i]+1000.0;
			}
			if (roi.softSieve[i] ==5){ //subCutFat
				SubCutFatA	+=1;
				SubCutFatD	+=roi.softScaledImage[i];
				//weightedFatArea += roi.softScaledImage[i]+1000.0;
			}
			if (roi.softSieve[i] ==6){ //Bone area
				BoneA	+=1;
				BoneD	+=roi.softScaledImage[i];
			}
			if (roi.softSieve[i] ==7){ //MedFat
				MeA	+=1;
				MeD	+=roi.softScaledImage[i];
			}
			if (roi.eroded[i] ==1){ //PeeledA
				PeeledA	+=1;
				PeeledD	+=roi.softScaledImage[i];
			}
			
		}
		LimbD/=LimbA;
		LimbA*=roi.pixelSpacing*roi.pixelSpacing/100.0;
		FatD/=FatA;
		FatA*=roi.pixelSpacing*roi.pixelSpacing/100.0;
		
		MeD/=MeA;
		MeA*=roi.pixelSpacing*roi.pixelSpacing/100.0;
		BoneD/=BoneA;
		BoneA*=roi.pixelSpacing*roi.pixelSpacing/100.0;
		PeeledD/=PeeledA;
		PeeledA*=roi.pixelSpacing*roi.pixelSpacing/100.0;
		
		//Added SubCutFatDMedian 2016/01/08
		double[] subCFatPixels = new double[(int) SubCutFatA];
		int cnt =0;
		for (int i =0;i<roi.width*roi.height;i++){
			if (roi.softSieve[i] ==5){ //subCutFat
				subCFatPixels[cnt]+=roi.softScaledImage[i];
				++cnt;
			}
		}
		SubCutFatDMedian = median(subCFatPixels);		
		SubCutFatD/=SubCutFatA;
		SubCutFatA*=roi.pixelSpacing*roi.pixelSpacing/100.0;
		MuD/=MuA;
		MuA*=roi.pixelSpacing*roi.pixelSpacing/100.0;
		TotalMuD/=TotalMuA;
		TotalMuA*=roi.pixelSpacing*roi.pixelSpacing/100.0;
		IntraMuFatD/=IntraMuFatA;
		IntraMuFatA*=roi.pixelSpacing*roi.pixelSpacing/100.0;
		FatPercentage = (weightedFatArea/weightedLimbArea)*100.0;
	}
	
	double median(double[] a) {
		if (a.length < 1){
			return 0d;
		}
		if (a.length ==1){
			return a[0];
		}
		Arrays.sort(a);
		int middle = a.length / 2;
		if (a.length % 2 == 0){
		  return (a[middle - 1] + a[middle]) / 2;
		}else{
		  return a[middle];
		}
	}
}