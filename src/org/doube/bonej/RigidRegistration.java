package org.doube.bonej;

import org.doube.registration.JointHistogram;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;

public class RigidRegistration implements PlugIn {

	private String title1 = "", title2 = "";

	public void run(String arg) {
		int[] wList = WindowManager.getIDList();
		if (wList == null) {
			IJ.noImage();
			return;
		}
		IJ.register(ImageCalculator.class);
		String[] titles = new String[wList.length];
		for (int i = 0; i < wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (imp != null)
				titles[i] = imp.getTitle();
			else
				titles[i] = "";
		}
		GenericDialog gd = new GenericDialog("Joint Histogram", IJ
				.getInstance());
		String defaultItem;
		if (title1.equals(""))
			defaultItem = titles[0];
		else
			defaultItem = title1;
		gd.addChoice("Image1:", titles, defaultItem);
		if (title2.equals(""))
			defaultItem = titles[0];
		else
			defaultItem = title2;
		gd.addChoice("Image2:", titles, defaultItem);
		gd.addNumericField("Min:", 0, 1, 6, "");
		gd.addNumericField("Max:", Double.POSITIVE_INFINITY, 1, 6, "");
		gd.addCheckbox("Auto limits", true);
		gd.addNumericField("Bins", 512, 0, 6, "");
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		int index1 = gd.getNextChoiceIndex();
		title1 = titles[index1];
		int index2 = gd.getNextChoiceIndex();
		title2 = titles[index2];
		ImagePlus img1 = WindowManager.getImage(wList[index1]);
		ImagePlus img2 = WindowManager.getImage(wList[index2]);
		float min = (float) gd.getNextNumber();
		float max = (float) gd.getNextNumber();
		boolean limit = gd.getNextBoolean();
		int nBins = (int) gd.getNextNumber();
		try {
			JointHistogram jh = new JointHistogram(img1, img2, nBins, min, max,
					limit);
			jh.calculate();
			ImagePlus img3 = jh.getJointHistogram();
			IJ.log("Shannon entropy of " + img1.getTitle() + " = "
					+ IJ.d2s(jh.getEntropy1(), 4));
			IJ.log("Shannon entropy of " + img2.getTitle() + " = "
					+ IJ.d2s(jh.getEntropy2(), 4));
			IJ.log("Shannon entropy of Joint Histogram = "
					+ IJ.d2s(jh.getEntropy3(), 4));
			IJ.log("Mutual information = " + IJ.d2s(jh.getMutualInfo(), 4));
			IJ.log("Normalised mutual information = "
					+ IJ.d2s(jh.getNormMutualInfo(), 4));
			if (img3 != null) {
				img3.show();
				IJ.run("Fire");
			}
		} catch (Exception e) {
			IJ.handleException(e);
			return;
		}
	}

}
