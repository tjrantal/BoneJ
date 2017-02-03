
package org.doube.bonej.pqct.utils;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;

public class VisualiseSieve{
	
	public static void showImage(double[] sieve,int width,int height){
		ImagePlus resultImage = NewImage.createFloatImage("Sieve",width,height,1, NewImage.FILL_BLACK);
		float[] rPixels = (float[])resultImage.getProcessor().getPixels();
		/*Convert the image to float*/
		double[] minmax = {Double.POSITIVE_INFINITY,Double.	NEGATIVE_INFINITY};
		for (int r = 0;r<height;++r){
			for (int c = 0;c<width;++c){
				rPixels[c+r*width] = (float) sieve[c+r*width];
				if (rPixels[c+r*width] < minmax[0]){
					minmax[0] = rPixels[c+r*width];
				}
				if (rPixels[c+r*width] > minmax[1]){
					minmax[1] = rPixels[c+r*width];
				}
			}
		}

		/*Visualize result*/
		resultImage.setDisplayRange(minmax[0],minmax[1]);
		resultImage.show();
	}
}