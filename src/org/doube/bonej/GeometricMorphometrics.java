package org.doube.bonej;

import java.awt.Component;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowListener;

import javax.media.j3d.View;
import javax.vecmath.Color3f;

import org.doube.util.ImageCheck;

import orthoslice.OrthoGroup;

import ij.Executer;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ScrollbarWithLabel;
import ij.plugin.Orthogonal_Views;
import ij.plugin.PlugIn;
import ij3d.AxisConstants;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.UniverseListener;

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
public class GeometricMorphometrics implements PlugIn, UniverseListener,
		MouseListener, KeyListener {
	private Image3DUniverse univ;
	private Orthogonal_Views orthoViewer;
	private ImagePlus imp;
	private ImageCanvas canvas;
	private OrthoGroup ortho3D;
	/** Position of the orthoviewers */
	private int x, y, z;
	private int resampling = 2;

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
		canvas = imp.getCanvas();
		addListeners();
	}

	private void addListeners() {
		univ.addUniverseListener(this);
		canvas.addMouseListener(this);
		// canvas.addMouseMotionListener(this);
		canvas.addKeyListener(this);
	}

	private void show3DOrtho() {
		String orthoTitle = "Ortho " + imp.getTitle();
		Content c = univ.getContent(orthoTitle);
		if (c == null) {
			try {
				univ.addOrthoslice(imp, (new Color3f(1.0f, 1.0f, 1.0f)),
						orthoTitle, 0, (new boolean[] { true, true, true }),
						resampling).setLocked(true);
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
						resampling).setLocked(true);
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

	/**
	 * Updates the 2D and 3D orthoviewers so they are displaying the same slices
	 */
	private void syncViewers() {
		// x, y and z are at the last synched position
		IJ.log("Start: (" + x + ", " + y + ", " + z + ")");
		// get the 2D orthoviewer's state
		int[] crossLoc = orthoViewer.getCrossLoc();
		int x2 = crossLoc[0];
		int y2 = crossLoc[1];
		int z2 = crossLoc[2];

		// if the change was in the 2D viewer, update the 3D viewer
		if (x2 != x || y2 != y || z2 != z) {
			x = x2;
			y = y2;
			z = z2;
			ortho3D.setSlice(AxisConstants.X_AXIS, x / resampling);
			ortho3D.setSlice(AxisConstants.Y_AXIS, y / resampling);
			ortho3D.setSlice(AxisConstants.Z_AXIS, z / resampling - resampling);
			IJ.log("End: (" + x + ", " + y + ", " + z + ")");
			return;
		}

		// get the 3D orthoviewer's state
		int x3 = ortho3D.getSlice(AxisConstants.X_AXIS) * resampling;
		int y3 = ortho3D.getSlice(AxisConstants.Y_AXIS) * resampling;
		int z3 = (ortho3D.getSlice(AxisConstants.Z_AXIS) + 1) * resampling;

		// if the change was in the 3D viewer, update the 2D viewer
		// have to be able to handle differences due to resampling
		if (x3 != x || y3 != y || z3 != z) {
			x = x3;
			y = y3;
			z = z3;
			orthoViewer.setCrossLoc(x, y, z);
			IJ.log("End: (" + x + ", " + y + ", " + z + ")");
			return;
		}
		return;
	}

	@Override
	public void canvasResized() {
		syncViewers();
	}

	@Override
	public void contentAdded(Content c) {
		syncViewers();

	}

	@Override
	public void contentChanged(Content c) {
		syncViewers();

	}

	@Override
	public void contentRemoved(Content c) {
		syncViewers();

	}

	@Override
	public void contentSelected(Content c) {
		syncViewers();

	}

	@Override
	public void transformationFinished(View view) {
		syncViewers();

	}

	@Override
	public void transformationStarted(View view) {
		syncViewers();

	}

	@Override
	public void transformationUpdated(View view) {
		syncViewers();

	}

	@Override
	public void universeClosed() {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		syncViewers();
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
		syncViewers();
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}
}
