package se.sics.mspsim.util;

public class Tuple {
	
	private double x;
	private double y;
	
	public Tuple(double x, double y) {
		this.setX(x);
		this.setY(y);
	}

	public void setX(double val) {
		this.x = val;
	}

	public double getX() {
		return x;
	}

	public void setY(double val) {
		this.y = val;
	}

	public double getY() {
		return y;
	}
	
	public String toString() {
		return (new Double(x).toString() + " " + new Double(y).toString());
	}

}
