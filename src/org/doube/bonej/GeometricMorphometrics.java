package org.doube.bonej;

import javax.vecmath.Color3f;

import org.doube.util.ImageCheck;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Orthogonal_Views;
import ij.plugin.PlugIn;
import ij3d.Image3DUniverse;

/**
 * 
 * Create landmark sets and analyse them using ImageJ's Orthoslice viewer and 3D
 * Viewer.
 * 
 * Targeting: Generation of labelled landmark sets Storing of landmark
 * definitions Storing of landmarks (in TIFF header?) Viewing & editing
 * landmarks synchronised in orthoviewer and 3D viewer Registering stacks based
 * on landmark sets Analysis? PCA? 4D viewing of landmark sets from different
 * specimens
 * 
 * @author Michael Doube
 * 
 */
public class GeometricMorphometrics implements PlugIn {
	private Image3DUniverse univ;
	private Orthogonal_Views orthoViewer;
	private ImagePlus imp;

	public void run(String arg) {
		if (!ImageCheck.checkEnvironment())
			return;
		this.imp = IJ.getImage();
		if (this.imp == null)
			return;
		if (!(new ImageCheck()).isMultiSlice(imp)) {
			IJ.error("");
			return;
		}

		orthoViewer = new Orthogonal_Views();
		orthoViewer.run("");
		univ = new Image3DUniverse();
		show3DVolume();
		univ.show();
	}

	private void show3DOrtho() {
		try {
			univ.addOrthoslice(imp, (new Color3f(1.0f, 1.0f, 1.0f)),
					"Ortho " + imp.getTitle(), 0,
					(new boolean[] { true, true, true }), 2).setLocked(true);
		} catch (NullPointerException npe) {
			IJ.log("3D Viewer was closed before rendering completed.");
		}
	}

	private void show3DVolume() {
		try {
			univ.addVoltex(imp, new Color3f(1.0f, 1.0f, 1.0f), imp.getTitle(),
					0, new boolean[] { true, true, true }, 2).setLocked(true);
		} catch (NullPointerException npe) {
			IJ.log("3D Viewer was closed before rendering completed.");
		}
	}
}
