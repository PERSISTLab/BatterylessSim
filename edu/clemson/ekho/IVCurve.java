package edu.clemson.ekho;

import java.util.List;

public class IVCurve {
	
	private List<Double> currents;
	
	public IVCurve (List<Double> currentList) {
		currents = currentList;
	}
	
	/**
	 * Returns the FIRST index, so cant exceed size() - 2
	 * @param current
	 * @return
	 */
	public int getIndex (double current) {;
		for (int index = 0; index < currents.size() - 1; index++)
		{
			if (currents.get(index) >= current && currents.get(index + 1) <= current) {
				break;
			}
		}
		// If current is greater than possible, then return max, this will set the voltage to zero so should not matter 
		return currents.size() - 2;
	}
	
	public double getCurrent (int index) {
		return currents.get(index);
	}
	
	public double getInterpolatedCurrent (int index) {
		return currents.get(index + 1) - currents.get(index);
	}
	
	public String toString() {
		String ret = "";
		for (Double current : currents) {
			ret += current + "\t";
		}
		return ret;
	}
}
