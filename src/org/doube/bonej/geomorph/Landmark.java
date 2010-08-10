package org.doube.bonej.geomorph;

/**
 * Landmark class for storing multidimensional points
 * 
 * @author Michael Doube
 * 
 */
public class Landmark {
	private double x, y, z;
	private long t;
	private String spaceUnit;
	private String name;

	public Landmark(double x, double y, double z, String name) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.name = name;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	public long getT() {
		return t;
	}
	
	public String getName(){
		return name;
	}

	public void setPos(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void setTime(long t) {
		this.t = t;
	}
	
	public void setName(String name){
		this.name = name;
	}
}
