package org.doube.registration;

import java.util.concurrent.atomic.AtomicInteger;

import org.doube.bonej.Moments;
import org.doube.jama.Matrix;
import org.doube.util.Multithreader;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public class Transformer {

	public static ImagePlus rotate(ImagePlus imp, double[][] rotation) {
		ImageStack source = imp.getImageStack();
		final int w = source.getWidth();
		final int h = source.getHeight();
		final int d = source.getSize();
		final double xc = (double) w / 2;
		final double yc = (double) h / 2;
		final double zc = (double) d / 2;
		Matrix R = new Matrix(rotation);
		Matrix Ri = R.inverse();
		final double[][] ri = Ri.getArrayCopy();
		final double ri00 = ri[0][0];
		final double ri01 = ri[0][1];
		final double ri02 = ri[0][2];
		final double ri10 = ri[1][0];
		final double ri11 = ri[1][1];
		final double ri12 = ri[1][2];
		final double ri20 = ri[2][0];
		final double ri21 = ri[2][1];
		final double ri22 = ri[2][2];

		final ImageProcessor[] sourceIp = new ImageProcessor[d + 1];
		for (int z = 1; z <= d; z++)
			sourceIp[z] = source.getProcessor(z);

		ImageStack target = new ImageStack(w, h, d);
		final ImageProcessor[] targetProcessors = new ImageProcessor[d + 1];
		final int b = imp.getBitDepth();
		for (int z = 1; z <= d; z++) {
			target.setPixels(Moments.getEmptyPixels(w, h, b), z);
			targetProcessors[z] = target.getProcessor(z);
		}

		final AtomicInteger ai = new AtomicInteger(1);
		Thread[] threads = Multithreader.newThreads();

		for (int thread = 0; thread < threads.length; thread++) {
			threads[thread] = new Thread(new Runnable() {
				public void run() {
					for (int z = ai.getAndIncrement(); z <= d; z = ai
							.getAndIncrement()) {
						ImageProcessor tip = targetProcessors[z];
						final double zD = z - zc;
						final double zDri00 = zD * ri20;
						final double zDri01 = zD * ri21;
						final double zDri02 = zD * ri22;
						for (int y = 0; y < h; y++) {
							final double yD = y - yc;
							final double yDri10 = yD * ri10;
							final double yDri11 = yD * ri11;
							final double yDri12 = yD * ri12;
							for (int x = 0; x < w; x++) {
								final double xD = x - xc;
								final double xAlign = xD * ri00 + yDri10
										+ zDri00 + xc;
								final double yAlign = xD * ri01 + yDri11
										+ zDri01 + yc;
								final double zAlign = xD * ri02 + yDri12
										+ zDri02 + zc;
								final int xA = (int) Math.floor(xAlign);
								final int yA = (int) Math.floor(yAlign);
								final int zA = (int) Math.floor(zAlign);

								if (xA < 0 || xA >= w || yA < 0 || yA >= h
										|| zA < 1 || zA > d) {
									continue;
								} else {
									tip.set(x, y, sourceIp[zA].get(xA, yA));
								}
							}
						}
					}
				}
			});
		}
		Multithreader.startAndJoin(threads);

		ImagePlus out = new ImagePlus("Rotated " + imp.getTitle(), target);
		out.setCalibration(imp.getCalibration());
		out.setDisplayRange(imp.getDisplayRangeMin(), imp.getDisplayRangeMax());
		return out;
	}

	/**
	 * @param imp
	 * @param t
	 *            Absolute translation in voxel units
	 * @return
	 */
	public static ImagePlus translate(ImagePlus imp, double[] t) {
		ImageStack source = imp.getImageStack();
		final int w = source.getWidth();
		final int h = source.getHeight();
		final int d = source.getSize();
		final double xt = t[0];
		final double yt = t[1];
		final double zt = t[2];

		final ImageProcessor[] sourceIp = new ImageProcessor[d + 1];
		for (int z = 1; z <= d; z++)
			sourceIp[z] = source.getProcessor(z);

		ImageStack target = new ImageStack(w, h, d);
		final ImageProcessor[] targetProcessors = new ImageProcessor[d + 1];
		final int b = imp.getBitDepth();
		for (int z = 1; z <= d; z++) {
			target.setPixels(Moments.getEmptyPixels(w, h, b), z);
			targetProcessors[z] = target.getProcessor(z);
		}

		final AtomicInteger ai = new AtomicInteger(1);
		Thread[] threads = Multithreader.newThreads();

		for (int thread = 0; thread < threads.length; thread++) {
			threads[thread] = new Thread(new Runnable() {
				public void run() {
					for (int z = ai.getAndIncrement(); z <= d; z = ai
							.getAndIncrement()) {
						ImageProcessor tip = targetProcessors[z];
						final double zD = z - zt;
						final int zA = (int) Math.floor(zD);
						for (int y = 0; y < h; y++) {
							final double yD = y - yt;
							final int yA = (int) Math.floor(yD);
							for (int x = 0; x < w; x++) {
								final double xD = x - xt;
								final int xA = (int) Math.floor(xD);
								if (xA < 0 || xA >= w || yA < 0 || yA >= h
										|| zA < 1 || zA > d) {
									continue;
								} else {
									tip.set(x, y, sourceIp[zA].get(xA, yA));
								}
							}
						}
					}
				}
			});
		}
		Multithreader.startAndJoin(threads);

		ImagePlus out = new ImagePlus("Translated " + imp.getTitle(), target);
		out.setCalibration(imp.getCalibration());
		out.setDisplayRange(imp.getDisplayRangeMin(), imp.getDisplayRangeMax());
		return out;
	}
}
