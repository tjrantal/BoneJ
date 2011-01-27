package org.doube.geometry;

import java.util.ArrayList;

public class Rotation {

	public static final double[] NORTH = { 0, 0, 1 };

	/** Generate a list of random rotation matrices */
	public static ArrayList<double[][]> randomRotations(int nRotations) {
		double[][] vectors = Vectors.randomVectors(nRotations);
		ArrayList<double[][]> rotations = new ArrayList<double[][]>(nRotations);
		for (int i = 0; i < nRotations; i++)
			rotations.add(vectorToRotation(vectors[i]));
		return rotations;
	}

	/**
	 * Calculate the rotation matrix that rotates the north unit vector [0,0,1]
	 * to the position of the input vector
	 * 
	 * @param vector
	 * @return
	 */
	public static double[][] vectorToRotation(double[] vector) {
		final double[] axis = Vectors.crossProduct(vector, NORTH);
		final double angle = Vectors.angle(vector, NORTH);
		double[][] rotation = axisAngleToRotation(axis, angle);
		return rotation;
	}

	/**
	 * Convert axis-angle to rotation matrix
	 * 
	 * @param axis
	 *            vector of axis
	 * @param angle
	 *            angle of rotation
	 * @see http://www.euclideanspace.com/maths/geometry/rotations/conversions/
	 *      angleToMatrix/index.htm
	 * @return
	 */
	public static double[][] axisAngleToRotation(double[] axis, double angle) {
		final double c = Math.cos(angle);
		final double s = Math.sin(angle);
		final double t = 1 - c;
		final double d = Trig.distance3D(axis);
		final double x = axis[0] / d;
		final double y = axis[1] / d;
		final double z = axis[2] / d;
		double[][] r = new double[3][3];

		r[0][0] = t * x * x + c;
		r[0][1] = t * x * y - z * s;
		r[0][2] = t * x * z + y * s;
		r[1][0] = t * x * y + z * s;
		r[1][1] = t * y * y + c;
		r[1][2] = t * y * z - x * s;
		r[2][0] = t * x * z - y * s;
		r[2][1] = t * y * z + x * s;
		r[2][2] = t * z * z + c;
		return r;
	}
	
	/**
	 * Rotate a 3D vector by a rotation matrix
	 *  
	 * @param vector
	 * @param rot Rotation matrix
	 * @return 
	 */
	public static double[] rotate(double[] vector, double[][] rot){
		final double x = vector[0];
		final double y = vector[1];
		final double z = vector[2];
		double[] rotated = new double[3];
		rotated[0] = x *rot[0][0] + y * rot[1][0] + z * rot[2][0];
		rotated[1] = x *rot[0][1] + y * rot[1][1] + z * rot[2][1];
		rotated[2] = x *rot[0][2] + y * rot[1][2] + z * rot[2][2];
		return rotated;
	}

}
