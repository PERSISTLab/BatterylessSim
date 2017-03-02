package edu.clemson.ekho;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Ekho {

	private int numbcolumns;
	private static final String COMMENT = "#";
	private IVSurface ivSurface;
	private String name;
	
	public Ekho(String ekhotrace) {
		ivSurface = new IVSurface();
		name = ekhotrace;
		readIVSurface(ekhotrace);
	}
	
	public String getName () {
		return name;
	}
	
	public double getSurfaceEndTime() {
		return ivSurface.getMaxTime();
	}
	
	public IVSurface getIVSurface () {
		return ivSurface;
	}

	// Simple function to parse comments "#" out of file
	private String removeComment (String value) {
		if (value.contains(COMMENT)) {
			return value.substring(0, value.indexOf(COMMENT)).trim();
		} else {
			return value;
		}
	}
	
	public double getCurrent (double time, double voltage) {
		return ivSurface.getCurrent(time, voltage);
	}
	
	public void readIVSurface (String file) {
		String line = "";
		int linenumb = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((line = br.readLine()) != null) {
				// Use comma as seperator
				String[] values = line.split(",");
				
				if (values[0].startsWith(COMMENT)) {
					// Do nothing and don't count this line
				} else {
					if (linenumb == 0) { // The first line is the number of columns per row
						int n = Integer.parseInt(removeComment(values[0]));
						numbcolumns = (int) (Math.pow(2, n) + 1);
						ivSurface.setRange(numbcolumns);
					} else if (linenumb == 1) { // The second line contains the min and max voltage
						ivSurface.setMinVoltage(removeComment(values[0]));
						ivSurface.setMaxVoltage(removeComment(values[1])); 
					} else { // These lines are all IVCurves
						double time = Double.parseDouble(removeComment(values[0]));
						List<Double> currents = new ArrayList<>();
						for (int i = 1; i < numbcolumns + 1; i++)
						{
							currents.add(Double.parseDouble(removeComment(values[i])));
						}
						ivSurface.addIVCurve(time, currents);
					}
					linenumb++;
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ivSurface.setup();
		}
	}
	
	public double getVoltage (double time, double current) {
		double vl = ivSurface.getVoltage(time, current);
		return vl;
	}
	
	public static void main(String args[]) {
		Ekho ekho = new Ekho("ekho/surfaces/am1456.csv");
		//ekho.readIVSurface("ekho/surfaces/example.csv");
		//System.out.println(ekho.getIVSurface().toString());
		System.out.println(ekho.getIVSurface().getVoltage(0.0, 0.0));
	}

	public double getMaximumVoltage() {
		return ivSurface.getMaxVoltage();
	}
}
