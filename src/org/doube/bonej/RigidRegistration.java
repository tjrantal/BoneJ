package org.doube.bonej;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import org.doube.geometry.Rotation;
import org.doube.jama.Matrix;
import org.doube.registration.JointHistogram;
import org.doube.registration.Transformer;

import customnode.CustomPointMesh;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij3d.Content;
import ij3d.Image3DUniverse;

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

	private ImagePlus register(ImagePlus img1, ImagePlus img2, int nBins,
			float min, float max, boolean limit) {
		JointHistogram jh = new JointHistogram(img1, img2, nBins, min, max,
				limit);
		jh.calculate();
		double nmi = jh.getNormMutualInfo();
		double maxNmi = nmi;
		int besti = 0;
		int bestj = 0;
		IJ.log("Starting NMI = " + IJ.d2s(nmi, 5));
		ArrayList<double[][]> rotations = Rotation.randomRotations(64);
		// ArrayList<double[]> translations = randomTranslations(64);
		final double w = (double) img2.getWidth();
		final double h = (double) img2.getHeight();
		final double d = (double) img2.getStackSize();
		final int nX = 8;
		final int nY = 8;
		final int nZ = 8;
		double s = 0.25;
		ArrayList<double[]> translations = gridTranslations(nX, nY, nZ, -w * s,
				w * s, -h * s, h * s, -d * s, d * s);

		ArrayList<Double> nmis = new ArrayList<Double>(nX * nY * nZ);
		IJ.log("Number of translations: " + translations.size());
		// for (int i = 0; i < rotations.size(); i++) {
		// ImagePlus testImp = Transformer.rotate(img2, rotations.get(i));
		for (int j = 0; j < translations.size(); j++) {
			double[] t = translations.get(j);
			IJ.log("Translation: (" + t[0] + ", " + t[1] + ", " + t[2] + ")");
			ImagePlus testImp = Transformer.translate(img2, t);
			jh.setImg2(testImp);
			jh.calculate();
			// Matrix R = new Matrix(rotations.get(i));
			nmi = jh.getNormMutualInfo();
			if (nmi != Double.POSITIVE_INFINITY)
				nmis.add(new Double(nmi));
			else
				nmis.add(null);
			if (nmi > maxNmi && nmi != Double.POSITIVE_INFINITY) {
				// R.printToIJLog("i: " + i + ", j: " + j + ", NMI: " +
				// nmi);
				maxNmi = nmi;
				// besti = i;
				bestj = j;
			}
		}
		// }
		IJ.log("Best NMI was at rotation index " + besti
				+ ", translation index " + bestj);
		displayNmiGrid(nmis, translations);
		return Transformer.translate(img2, translations.get(bestj));
	}

	private void displayNmiGrid(ArrayList<Double> nmis,
			ArrayList<double[]> translations) {
		double maxNmi = 0;
		for (Double nmi : nmis)
			maxNmi = Math.max(nmi.doubleValue(), maxNmi);
		final int nPoints = nmis.size();
		Image3DUniverse univ = new Image3DUniverse();
		for (int i = 0; i < nPoints; i++) {
			Double nmi = nmis.get(i);
			if (nmi == null)
				continue;
			List<Point3f> mesh = new ArrayList<Point3f>();
			mesh.add(new Point3f((float) translations.get(i)[0],
					(float) translations.get(i)[1],
					(float) translations.get(i)[2]));
			CustomPointMesh cm = new CustomPointMesh(mesh);
			final float n = (float)(nmi.doubleValue() / maxNmi);
			cm.setPointSize(5.0f * n);
			cm.setColor(new Color3f(n, n, 1.0f - n));
			Content c = univ.addCustomMesh(cm, "" + i);
		}
		univ.show();

	}

	/**
	 * Create a list of unit translations in x, y, z
	 * 
	 * @param n
	 * @return
	 */
	private ArrayList<double[]> randomTranslations(int n) {
		ArrayList<double[]> translations = new ArrayList<double[]>(n);
		for (int i = 0; i < n; i++) {
			double[] t = { Math.random() * 2 - 1, Math.random() * 2 - 1,
					Math.random() * 2 - 1, };
			translations.add(t);
		}
		return translations;
	}

	/**
	 * Create a grid of translation vectors
	 * 
	 * @param nX
	 * @param nY
	 * @param nZ
	 * @param xMin
	 * @param xMax
	 * @param yMin
	 * @param yMax
	 * @param zMin
	 * @param zMax
	 * @return
	 */
	private ArrayList<double[]> gridTranslations(int nX, int nY, int nZ,
			double xMin, double xMax, double yMin, double yMax, double zMin,
			double zMax) {
		final double xInc = (xMax - xMin) / (double) (nX - 1);
		final double yInc = (yMax - yMin) / (double) (nY - 1);
		final double zInc = (zMax - zMin) / (double) (nZ - 1);
		ArrayList<double[]> translations = new ArrayList<double[]>(nX * nY * nZ);
		for (int z = 0; z < nZ; z++) {
			for (int y = 0; y < nY; y++) {
				for (int x = 0; x < nX; x++) {
					double[] t = { (double) x * xInc + xMin,
							(double) y * yInc + yMin, (double) z * zInc + zMin };
					translations.add(t);
				}
			}
		}
		return translations;
	}
}
