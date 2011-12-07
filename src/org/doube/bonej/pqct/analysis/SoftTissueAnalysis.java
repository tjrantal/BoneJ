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

public class SoftTissueAnalysis{
	public double MuA;
	public double FatA;
	public double LimbA;
	public double MuD;
	public double FatD;
	public double LimbD;
	public SoftTissueAnalysis(SelectROI roi){
		MuA		=0;
		FatA	=0;
		LimbA	=0;
		MuD		=0;
		FatD	=0;
		LimbD	=0;
		for (int i =0;i<roi.width*roi.height;i++){
			if (roi.softSieve[i] >0 && roi.softSieve[i] <4){
				LimbA +=1;
				LimbD +=roi.scaledImage[i];
			}
			if (roi.softSieve[i] ==2){
				FatA +=1;
				FatD +=roi.scaledImage[i];
			}
			if (roi.softSieve[i] ==3){
				MuA +=1;
				MuD +=roi.scaledImage[i];
			}
		}
		LimbD/=LimbA;
		LimbA*=roi.pixelSpacing*roi.pixelSpacing;
		FatD/=FatA;
		FatA*=roi.pixelSpacing*roi.pixelSpacing;
		MuD/=MuA;
		MuA*=roi.pixelSpacing*roi.pixelSpacing;
	}
}