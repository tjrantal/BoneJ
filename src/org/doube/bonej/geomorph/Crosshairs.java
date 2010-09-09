package org.doube.bonej.geomorph;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import customnode.CustomLineMesh;

import ij.IJ;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.UniverseListener;

/**
 * 
 * 
 * @author Michael Doube
 */
public class Crosshairs implements UniverseListener, KeyListener {
	private static final Transform3D emptyTransform = new Transform3D();
	private Image3DUniverse univ;
	private double x, y, z;
	private CustomLineMesh clmX, clmY, clmZ;
	private Content cX, cY, cZ;

	public Crosshairs(double x, double y, double z, Image3DUniverse univ) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.univ = univ;
		create();
	}

	private void create() {
		Point3d globalMax = new Point3d();
		univ.getGlobalMaxPoint(globalMax);

		Point3d globalMin = new Point3d();
		univ.getGlobalMinPoint(globalMin);

		ArrayList<Point3f> meshZ = new ArrayList<Point3f>();
		Point3f start1 = new Point3f();
		start1.x = (float) x;
		start1.y = (float) y;
		start1.z = (float) globalMin.z;
		meshZ.add(start1);

		Point3f end1 = new Point3f();
		end1.x = (float) x;
		end1.y = (float) y;
		end1.z = (float) globalMax.z;
		meshZ.add(end1);

		ArrayList<Point3f> meshY = new ArrayList<Point3f>();
		Point3f start2 = new Point3f();
		start2.x = (float) x;
		start2.y = (float) globalMin.y;
		start2.z = (float) z;
		meshY.add(start2);

		Point3f end2 = new Point3f();
		end2.x = (float) x;
		end2.y = (float) globalMax.y;
		end2.z = (float) z;
		meshY.add(end2);

		ArrayList<Point3f> meshX = new ArrayList<Point3f>();
		Point3f start3 = new Point3f();
		start3.x = (float) globalMin.x;
		start3.y = (float) y;
		start3.z = (float) z;
		meshX.add(start3);

		Point3f end3 = new Point3f();
		end3.x = (float) globalMax.x;
		end3.y = (float) y;
		end3.z = (float) z;
		meshX.add(end3);

		clmX = new customnode.CustomLineMesh(meshX);
		clmY = new customnode.CustomLineMesh(meshY);
		clmZ = new customnode.CustomLineMesh(meshZ);
		try {
			cX = univ.addCustomMesh(clmX, "Crosshairs X");
			cY = univ.addCustomMesh(clmY, "Crosshairs Y");
			cZ = univ.addCustomMesh(clmZ, "Crosshairs Z");
			resetColor();
			cX.setLocked(true);
			cY.setLocked(true);
			cZ.setLocked(true);
		} catch (NullPointerException npe) {
			IJ.log("3D Viewer was closed before rendering completed.");
			return;
		}
		univ.addUniverseListener(this);
		univ.getCanvas().addKeyListener(this);
	}

	public void set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void update() {

		Point3d globalMax = new Point3d();
		univ.getGlobalMaxPoint(globalMax);

		Point3d globalMin = new Point3d();
		univ.getGlobalMinPoint(globalMin);

		Point3f start1 = new Point3f();
		start1.x = (float) x;
		start1.y = (float) y;
		start1.z = (float) globalMin.z;

		Point3f end1 = new Point3f();
		end1.x = (float) x;
		end1.y = (float) y;
		end1.z = (float) globalMax.z;

		Point3f start2 = new Point3f();
		start2.x = (float) x;
		start2.y = (float) globalMin.y;
		start2.z = (float) z;

		Point3f end2 = new Point3f();
		end2.x = (float) x;
		end2.y = (float) globalMax.y;
		end2.z = (float) z;

		Point3f start3 = new Point3f();
		start3.x = (float) globalMin.x;
		start3.y = (float) y;
		start3.z = (float) z;

		Point3f end3 = new Point3f();
		end3.x = (float) globalMax.x;
		end3.y = (float) y;
		end3.z = (float) z;

		clmZ.setCoordinate(0, start1);
		clmZ.setCoordinate(1, end1);
		clmY.setCoordinate(0, start2);
		clmY.setCoordinate(1, end2);
		clmX.setCoordinate(0, start3);
		clmX.setCoordinate(1, end3);
		
		cX.setTransform(emptyTransform);
		cY.setTransform(emptyTransform);
		cZ.setTransform(emptyTransform);
		
	}

	public void show() {
		cX.setVisible(true);
		cY.setVisible(true);
		cZ.setVisible(true);
	}

	public void hide() {
		cX.setVisible(false);
		cY.setVisible(false);
		cZ.setVisible(false);
	}

	private void resetColor() {
		Color3f yellow = new Color3f(1.0f, 1.0f, 0.0f);
		cX.setColor(yellow);
		cY.setColor(yellow);
		cZ.setColor(yellow);
	}
	
	@Override
	public void canvasResized() {
		// TODO Auto-generated method stub

	}

	@Override
	public void contentAdded(Content c) {
		// TODO Auto-generated method stub

	}

	@Override
	public void contentChanged(Content c) {
		IJ.log("Content changed: " + c.getName());
	}

	@Override
	public void contentRemoved(Content c) {
		// TODO Auto-generated method stub

	}

	@Override
	public void contentSelected(Content c) {
		resetColor();
		Color3f red = new Color3f(1.0f, 0.0f, 0.0f);
		if (c.equals(cX))
			cX.setColor(red);
		else if (c.equals(cY))
			cY.setColor(red);
		else if (c.equals(cZ))
			cZ.setColor(red);
		else
			return;
	}

	@Override
	public void transformationFinished(View view) {
		IJ.log("Transformation finished");
	}

	@Override
	public void transformationStarted(View view) {
		IJ.log("Transformation started");
	}

	@Override
	public void transformationUpdated(View view) {
		IJ.log("Transformation updated");
	}

	@Override
	public void universeClosed() {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyPressed(KeyEvent e) {
		// allow translation when shift is pressed
		if (e.getKeyCode() == 16) {
			cX.setLocked(false);
			cY.setLocked(false);
			cZ.setLocked(false);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// prevent rotation
		if (e.getKeyCode() == 16) {
			resetColor();
			cX.setSelected(false);
			cY.setSelected(false);
			cZ.setSelected(false);
			cX.setLocked(true);
			cY.setLocked(true);
			cZ.setLocked(true);
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}
}
