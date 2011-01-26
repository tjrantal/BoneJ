package org.doube.bonej;

import java.util.ArrayList;

import org.doube.geometry.Rotation;
import org.doube.jama.Matrix;
import org.doube.registration.JointHistogram;
import org.doube.registration.Transformer;

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
			ImagePlus img3 = register(img1, img2, nBins, min, max, limit);
			if (img3 != null)
				img3.show();
		} catch (Exception e) {
			IJ.handleException(e);
			return;
		}
	}

	private ImagePlus register(ImagePlus img1, ImagePlus img2, int nBins, float min,
			float max, boolean limit) {
		JointHistogram jh = new JointHistogram(img1, img2, nBins, min, max,
				limit);
		jh.calculate();
		double nmi = jh.getNormMutualInfo();
		IJ.log("Starting NMI = "+IJ.d2s(nmi, 5));
		ArrayList<double[][]> rotations = Rotation.randomRotations(1);
		
		for (int i = 0; i < rotations.size(); i++){
			ImagePlus testImp = Transformer.rotate(img2, rotations.get(i));
			jh.setImg2(testImp);
			jh.calculate();
			Matrix R = new Matrix(rotations.get(i));
			R.printToIJLog("i: "+i+", NMI: "+jh.getNormMutualInfo());
			testImp.show();
		}				
		return null; //TODO return an actual imp 
	}
}
