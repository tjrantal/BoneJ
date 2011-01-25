package org.doube.registration;

import org.doube.bonej.ThresholdMinConn;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Calculates the Shannon entropy of two input images and of their joint
 * histogram. From the 3 entropies, calculates the (normalised) mutual
 * information.
 * 
 * @author Michael Doube
 * 
 */
public class JointHistogram {

	/** The fixed image */
	private ImagePlus img1;

	/** The moving image */
	private ImagePlus img2;

	private boolean autoLimit;
	private double min;
	private double max;
	private int nBins;

	/** Shannon entropy of the fixed image */
	private double entropy1 = -1;
	/** Shannon entropy of the moving image */
	private double entropy2;
	/** Shannon entropy of the joint histogram */
	private double entropy3;

	private double mutualInfo;
	private double normMutualInfo;

	private int[] hist1;

	public JointHistogram(ImagePlus img1, ImagePlus img2, int nBins, float min,
			float max, boolean limit) {
		if (img1.getType() != img2.getType())
			throw new IllegalArgumentException("Image types must match");
		if (img1.getWidth() != img2.getWidth()
				|| img1.getHeight() != img2.getHeight()
				|| img1.getStackSize() != img2.getStackSize())
			throw new IllegalArgumentException("Image dimensions must match");
		setImg1(img1);
		setImg2(img2);
		this.nBins = nBins;
		this.min = min;
		this.max = max;
		this.autoLimit = limit;
	}

	public ImagePlus calculate() {
		ThresholdMinConn tmc = new ThresholdMinConn();
		if (hist1 == null)
			hist1 = tmc.getStackHistogram(img1);
		int[] hist2 = tmc.getStackHistogram(getImg2());

		if (autoLimit) {
			min = getHistogramMin(hist1);
			min = Math.min(min, getHistogramMin(hist2));
			max = getHistogramMax(hist1);
			max = Math.max(max, getHistogramMax(hist2));
		}
		FloatProcessor ip = new FloatProcessor(nBins, nBins);

		final int w = img1.getWidth();
		final int h = img1.getHeight();
		final int d = img1.getStackSize();

		ImageStack stack1 = img1.getImageStack();
		ImageStack stack2 = img2.getImageStack();
		for (int z = 1; z <= d; z++) {
			ImageProcessor ip1 = stack1.getProcessor(z);
			ImageProcessor ip2 = stack2.getProcessor(z);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					final float p1 = ip1.getf(x, y);
					final float p2 = ip2.getf(x, y);
					if (p1 < min || p1 > max || p2 < min || p2 > max)
						continue;
					final int bin1 = sort(p1, nBins, min, max);
					final int bin2 = sort(p2, nBins, min, max);
					ip.setf(bin1, bin2, ip.getf(bin1, bin2) + 1);
				}
			}
		}

		final int l = nBins * nBins;
		final double hMin = ip.getMin();
		final double hMax = ip.getMax();
		int[] hist3 = new int[nBins];
		for (int i = 0; i < l; i++) {
			final float value = ip.getf(i);
			hist3[sort(value, nBins, hMin, hMax)]++;
		}

		if (getEntropy1() < 0)
			entropy1 = shannonEntropy(hist1);
		entropy2 = shannonEntropy(hist2);
		entropy3 = shannonEntropy(hist3);

		// Registration occurs when mutualInformation is maximal
		mutualInfo = entropy1 + entropy2 - entropy3;

		// Or when NMI is maximal
		normMutualInfo = (entropy1 + entropy2) / entropy3;

		ImagePlus imp = new ImagePlus("Joint Histogram", ip);
		Calibration cal = new Calibration();
		cal.setValueUnit("count");
		cal.setXUnit(img1.getCalibration().getValueUnit());
		cal.setYUnit(getImg2().getCalibration().getValueUnit());
		cal.pixelWidth = cal.pixelHeight = (max - min) / nBins;
		cal.xOrigin = -min / cal.pixelWidth;
		cal.yOrigin = -min / cal.pixelHeight;
		imp.setCalibration(cal);
		return imp;
	}

	private static double getHistogramMin(int[] histogram) {
		int i = 0;
		while (histogram[i] == 0)
			i++;
		return i;
	}

	private static double getHistogramMax(int[] histogram) {
		int i = histogram.length - 1;
		while (histogram[i] == 0)
			i--;
		return i;
	}

	/**
	 * Sort the input value into a bin
	 * 
	 * @param value
	 * @return
	 */
	private static int sort(float value, int nBins, double min, double max) {
		return (int) Math.floor(nBins * (value - min) / (max - min + 1));
	}

	/**
	 * Calculate the Shannon entropy of a histogram
	 * 
	 * @param histogram
	 * @return Shannon entropy
	 */
	private static double shannonEntropy(int[] histogram) {
		double entropy = 0;
		double sum = 0;
		for (int i : histogram)
			sum += i;
		for (int i : histogram) {
			if (i == 0)
				continue;
			final double freq = i / sum;
			entropy -= Math.log(freq) * freq;
		}
		return entropy;
	}

	/**
	 * @return the mutualInfo
	 */
	public double getMutualInfo() {
		return mutualInfo;
	}

	/**
	 * @return the normMutualInfo
	 */
	public double getNormMutualInfo() {
		return normMutualInfo;
	}

	/**
	 * @param img1
	 *            the img1 to set
	 */
	public void setImg1(ImagePlus img1) {
		this.img1 = img1;
		this.entropy1 = -1; // reset entropy
		this.hist1 = null; // and histogram to allow recalculation
	}

	/**
	 * @return the img1
	 */
	public ImagePlus getImg1() {
		return img1;
	}

	/**
	 * @param img2
	 *            the img2 to set
	 */
	public void setImg2(ImagePlus img2) {
		this.img2 = img2;
	}

	/**
	 * @return the img2
	 */
	public ImagePlus getImg2() {
		return img2;
	}

	/**
	 * @return the entropy1
	 */
	public double getEntropy1() {
		return entropy1;
	}

	/**
	 * @return the entropy2
	 */
	public double getEntropy2() {
		return entropy2;
	}

	/**
	 * @return the entropy3
	 */
	public double getEntropy3() {
		return entropy3;
	}

}
