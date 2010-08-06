package org.doube.bonej;

import java.awt.event.MouseEvent;

import javax.vecmath.Color3f;

import org.doube.util.ImageCheck;

import orthoslice.OrthoGroup;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Orthogonal_Views;
import ij.plugin.PlugIn;
import ij3d.AxisConstants;
import ij3d.Content;
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
	private OrthoGroup ortho3D;

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
		show3DOrtho();
		univ.show();
		orthoListener();
	}

	private void show3DOrtho() {
		String orthoTitle = "Ortho " + imp.getTitle();
		Content c = univ.getContent(orthoTitle);
		if (c == null) {
			try {
				univ.addOrthoslice(imp, (new Color3f(1.0f, 1.0f, 1.0f)),
						orthoTitle, 0, (new boolean[] { true, true, true }), 2)
						.setLocked(true);
				c = univ.getContent(orthoTitle);
				ortho3D = (OrthoGroup) c.getContent();
			} catch (NullPointerException npe) {
				IJ.log("3D Viewer was closed before rendering completed.");
			}
		} else
			c.setVisible(true);
	}

	private void hide3DOrtho() {
		Content c = univ.getContent("Ortho " + imp.getTitle());
		if (c != null && c.isVisible()) {
			c.setVisible(false);
		}
	}

	private void show3DVolume() {
		Content c = univ.getContent("Ortho " + imp.getTitle());
		if (c == null) {
			try {
				univ.addVoltex(imp, new Color3f(1.0f, 1.0f, 1.0f),
						imp.getTitle(), 0, new boolean[] { true, true, true },
						2).setLocked(true);
			} catch (NullPointerException npe) {
				IJ.log("3D Viewer was closed before rendering completed.");
			}
		} else
			c.setVisible(true);
	}

	private void hide3DVolume() {
		Content c = univ.getContent(imp.getTitle());
		if (c != null && c.isVisible()) {
			c.setVisible(false);
		}
	}

	private void orthoListener() {
		// listen for changes to the 2D orthoviewer's state and update the
		// 3D orthoviewer position accordingly
		int x2 = 5, y2 = 10, z2 = 15;// test values
		ortho3D.setSlice(AxisConstants.X_AXIS, x2);
		ortho3D.setSlice(AxisConstants.Y_AXIS, y2);
		ortho3D.setSlice(AxisConstants.Z_AXIS, z2);

		// also listen for changes to the 3D ortho state and update the 2D
		// viewer accordingly
		int x3, y3, z3;
		x3 = ortho3D.getSlice(AxisConstants.X_AXIS);
		y3 = ortho3D.getSlice(AxisConstants.Y_AXIS);
		z3 = ortho3D.getSlice(AxisConstants.Z_AXIS);
		
		this.imp.setSlice(z3);
		orthoViewer.imageUpdated(imp);
	
	
	}
}
