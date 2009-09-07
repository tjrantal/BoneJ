package org.doube.bonej;

import java.util.Arrays;

import ij.IJ;

import Jama.Matrix;
import Jama.EigenvalueDecomposition;
import Jama.SingularValueDecomposition;

/**
 * Methods for fitting circles to coordinates
 * 
 * @author Michael Doube, ported from Nikolai Chernov's MATLAB scripts
 * @see <p>
 *      Al-Sharadqha & Chernov (2009) <a href="http://dx.doi.org/10.1214/09-EJS419"> Error
 *      analysis for circle fitting algorithms</a>.  Electronic Journal of Statistics 3, pp. 886-911<br/><a
 *      href="http://www.math.uab.edu/~chernov/cl/MATLABcircle.html"
 *      >http://www.math.uab.edu/~chernov/cl/MATLABcircle.html</a>
 *      </p>
 * 
 */
public class FitCircle {


    /**
     * Chernov's non-biased Hyper algebraic method. Simple version.
     * 
     * @see <p>
     *      <a
     *      href="http://www.math.uab.edu/~chernov/cl/HyperSVD.m">http://www.math
     *      .uab.edu/~chernov/cl/HyperSVD.m</a>
     *      </p>
     * 
     * @param points
     *            double[n][2] containing n (<i>x</i>, <i>y</i>) coordinates
     * @return 3-element double[] containing (<i>x</i>, <i>y</i>) centre and
     *         circle radius
     */
    public double[] hyperCircleSimple(double[][] points) {
	int nPoints = points.length;

	double[][] zxy1 = new double[nPoints][4];
	double[] zxy1Sum = new double[4];
	double[] s = new double[4];
	for (int n = 0; n < nPoints; n++) {
	    zxy1[n][1] = points[n][0];
	    zxy1[n][2] = points[n][1];
	    zxy1[n][0] = zxy1[n][0] * zxy1[n][0] + zxy1[n][1] * zxy1[n][1];
	    zxy1[n][3] = 1;
	    zxy1Sum[0] += points[n][0];
	    zxy1Sum[1] += points[n][1];
	    zxy1Sum[2] += zxy1[n][3];
	}
	s[0] = zxy1Sum[0] / nPoints;
	s[1] = zxy1Sum[1] / nPoints;
	s[2] = zxy1Sum[2] / nPoints;
	s[3] = 1;

	Matrix ZXY1 = new Matrix(zxy1);
	Matrix M = (ZXY1.transpose()).times(ZXY1);

	//N = [8*S(1) 4*S(2) 4*S(3) 2; 4*S(2) 1 0 0; 4*S(3) 0 1 0; 2 0 0 0];

	double[][] n = {{8*s[0], 4*s[1], 4*s[2], 2}, {4*s[1], 1, 0, 0}, {4*s[2], 0, 1, 0}, {2, 0 ,0, 0}};
	Matrix N = new Matrix(n);
	//NM and NM2 should be identical only faster and more accurate to use NM2
	Matrix NM = (N.inverse()).times(M);
	Matrix NM2 = N.arrayLeftDivide(M);
	printMatrix(NM, "NM");
	printMatrix(NM2, "NM2");

	EigenvalueDecomposition ED = new EigenvalueDecomposition(NM);
	Matrix E = ED.getV();
	Matrix D = ED.getD();

	double[] diagD = new double[D.getColumnDimension()];
	for (int i = 0; i < D.getColumnDimension(); i++){
	    diagD[i] = D.get(i, i);
	}
	double[] diagDorig = Arrays.copyOf(diagD, diagD.length);
	Arrays.sort(diagD);
	int secondSmallest = Arrays.binarySearch(diagDorig, diagD[1]);

	if (diagD[0] > 0){
	    IJ.error("Error: the smallest e-value is positive...");
	} 
	if (diagD[1] < 0){ 
	    IJ.error("Error: the second smallest e-value is negative...");
	}

	Matrix A = E.getMatrix(0, E.getRowDimension() - 1,
		secondSmallest, secondSmallest);

	printMatrix(A, "A");

	double[] centreRadius = new double[3];

	centreRadius[0] = -1 * (A.get(1, 0) / A.get(0, 0)) / 2;
	centreRadius[1] = -1 * (A.get(2, 0) / A.get(0, 0)) / 2;

	double[][] a = A.getArray();
	centreRadius[2] = Math.sqrt(a[1][0] * a[1][0] + a[2][0] * a[2][0] - 4
		* a[0][0] * a[3][0])
		/ Math.abs(a[0][0]) / 2;

	return centreRadius;	
    }

    /**
     * Chernov's non-biased Hyper algebraic method. Stability optimised version.
     * 
     * @see <p>
     *      <a
     *      href="http://www.math.uab.edu/~chernov/cl/HyperSVD.m">http://www.math
     *      .uab.edu/~chernov/cl/HyperSVD.m</a>
     *      </p>
     * 
     * @param points
     *            double[n][2] containing n (<i>x</i>, <i>y</i>) coordinates
     * @return 3-element double[] containing (<i>x</i>, <i>y</i>) centre and
     *         circle radius
     */
    public double[] hyperCircleStable(double[][] points) {
	int nPoints = points.length;

	double[] centroid = getCentroid(points);

	double sumZ = 0;
	double[][] xyz1 = new double[nPoints][4];
	// centre data and assign vector values
	for (int n = 0; n < nPoints; n++) {
	    xyz1[n][0] = points[n][0] - centroid[0];
	    xyz1[n][1] = points[n][1] - centroid[1];
	    xyz1[n][2] = xyz1[n][0] * xyz1[n][0] + xyz1[n][1] * xyz1[n][1];
	    xyz1[n][3] = 1;
	    sumZ += xyz1[n][2];
	}

	Matrix XYZ1 = new Matrix(xyz1);
	SingularValueDecomposition svd = new SingularValueDecomposition(XYZ1);

	Matrix S = svd.getS();
	Matrix V = svd.getV();

	Matrix A;

	// singular case
	if (S.get(3, 3) / S.get(0, 0) < 1e-12) {
	    double[][] v = V.getArray();
	    double[][] a = new double[v.length][1];
	    for (int i = 0; i < a.length; i++) {
		a[i][0] = v[i][3];
	    }
	    A = new Matrix(a);
	    printMatrix(A, "A");
	} else {
	    // regular case
	    Matrix Y = V.times(S.times(V.transpose()));
	    printMatrix(Y, "Y");

	    double[][] bInv = { { 0, 0, 0, 0.5 }, { 0, 1, 0, 0 },
		    { 0, 0, 1, 0 }, { 0.5, 0, 0, -2 * sumZ / nPoints } };
	    Matrix Binv = new Matrix(bInv);
	    printMatrix(Binv, "Binv");

	    EigenvalueDecomposition ED = new EigenvalueDecomposition((Y
		    .transpose()).times(Binv.times(Y)));
	    Matrix D = ED.getD(); // eigenvalues
	    Matrix E = ED.getV(); // eigenvectors

	    printMatrix(D, "D");
	    printMatrix(E, "E");

	    // [Dsort,ID] = sort(diag(D));
	    // A = E(:,ID(2));
	    // I think these 2 lines mean "Make an array, A, out of the
	    // eigenvector corresponding to the 2nd smallest eigenvalue"

	    double[] diagD = new double[D.getColumnDimension()];
	    for (int i = 0; i < D.getColumnDimension(); i++){
		diagD[i] = D.get(i, i);
	    }
	    double[] diagDorig = Arrays.copyOf(diagD, diagD.length);
	    Arrays.sort(diagD);
	    int almostSmallest = Arrays.binarySearch(diagDorig, diagD[1]);
	    IJ.log("diagD = {"+diagD[0]+", "+diagD[1]+", "+diagD[2]+", "+diagD[3]+"}");
	    IJ.log("diagDorig = {"+diagDorig[0]+", "+diagDorig[1]+", "+diagDorig[2]+", "+diagDorig[3]+"}");


	    // get the 2nd to last column from eigenvectors
	    //	    A = E.getMatrix(0, E.getRowDimension() - 1,
	    //		    E.getColumnDimension()-2, E.getColumnDimension()-2);

	    A = E.getMatrix(0, E.getRowDimension() - 1,
		    almostSmallest, almostSmallest);

	    printMatrix(A, "A");

	    for (int i = 0; i < 4; i++) {
		double p = 1 / S.get(i, i);
		S.set(i, i, p);
	    }
	    A = V.times(S.times((V.transpose()).times(A)));
	    printMatrix(A, "A again");
	}

	double[] centreRadius = new double[3];
	// Par = [-(A(2:3))'/A(1)/2+centroid ,
	// sqrt(A(2)*A(2)+A(3)*A(3)-4*A(1)*A(4))/abs(A(1))/2];
	centreRadius[0] = -1 * (A.get(1, 0) / A.get(0, 0)) / 2 + centroid[0];
	centreRadius[1] = -1 * (A.get(2, 0) / A.get(0, 0)) / 2 + centroid[1];

	// radius
	double[][] a = A.getArray();
	centreRadius[2] = Math.sqrt(a[1][0] * a[1][0] + a[2][0] * a[2][0] - 4
		* a[0][0] * a[3][0])
		/ Math.abs(a[0][0]) / 2;

	IJ.log("Centroid is at ("+centroid[0]+", "+centroid[1]+")");
	return centreRadius;
    }

    private double[] getCentroid(double[][] points) {
	double[] centroid = new double[2];
	double sumX = 0;
	double sumY = 0;
	int nPoints = points.length;

	for (int n = 0; n < nPoints; n++) {
	    sumX += points[n][0];
	    sumY += points[n][1];
	}

	centroid[0] = sumX / nPoints;
	centroid[1] = sumY / nPoints;

	return centroid;
    }

    public void printMatrix(Matrix matrix, String title) {
	IJ.log(title);
	int nCols = matrix.getColumnDimension();
	int nRows = matrix.getRowDimension();
	double[][] eVal = matrix.getArrayCopy();
	for (int r = 0; r < nRows; r++) {
	    String row = "||";
	    for (int c = 0; c < nCols; c++) {
		row = row + eVal[r][c] + "|";
	    }
	    row = row + "|";
	    IJ.log(row);
	}
	return;
    }

}
