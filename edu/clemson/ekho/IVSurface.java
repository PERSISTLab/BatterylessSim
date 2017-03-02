package edu.clemson.ekho;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class IVSurface {

	// Map<time, IVCurve>
	private Map<Double, IVCurve> map;
	private double minvoltage;
	private double maxvoltage;
	private int range;
	private double [] voltages;
	private double maxTime;
	public IVSurface () {
		map = new TreeMap<>();
	}
	
	public void addIVCurve (double time, List<Double> currents) {
		IVCurve curve = new IVCurve (currents);
		map.put(time, curve);
		if(time > maxTime) maxTime = time;
	}
	
	public void setRange (int range) {
		this.range = range;
	}
	
	public double getMaxTime() {
		return maxTime;
	}
	
	public void setup () {
		voltages = new double[range];
		for (int i = 0; i < voltages.length; i++)
		{
			voltages[i] = ((maxvoltage - minvoltage) / range) * i;
//			System.out.println("i = " + i + " and voltage = " + voltages[i]);
		}
	}
	
	/*public double getCurrent (double time, double volt) {
		
		IVCurve curve = null;
		int index = 0;
		
		for (int i = 0; i < voltages.length - 1; i++) {
			if (voltages[i + 1] >= volt && voltages[i] <= volt) {
				index = i;
				break;
			}
		}
		
		for (Double ivTime : map.keySet())
		{
			if (ivTime > time) {
				curve = map.get(ivTime);
				break;
			}
		}
		
		if (curve != null) {
			double interCurrent = curve.getInterpolatedCurrent(index, index + 1);
			double interVoltage = voltages[index+1] - voltages[index];
			double m = interCurrent / interVoltage;
			System.out.println("m = " + m);
			double b = curve.getCurrent(index) / (m * voltages[index]);
			System.out.println("b = " + b + " and volt = " + volt);
			return m * volt + b;
			//int index = curve.getIndex(current);
			//return voltage[index];
		} else {
			System.out.println("Why is this IV curve null...?");
		}
		
		return -1;
	}*/

	public double getCurrent (double time, double voltage) {
		double retval = 0.0;
		IVCurve curve = null;
		
		for (Double ivTime : map.keySet())
		{
			if (ivTime > time) {
				curve = map.get(ivTime);
				//System.out.println(ivTime);
				break;
			}
		}
		
		if (curve != null) {
			int index=0;
			for(;index<voltages.length-1;index++) {
				if (voltages[index+1] >= voltage && voltages[index] <= voltage)
					break;
			}
			retval = curve.getCurrent(index);
		}
		
		return retval;
	}
	
	public double getVoltage (double time, double current) {
		double retval = 0.0;
		IVCurve curve = null;
		
		for (Double ivTime : map.keySet())
		{
			if (ivTime > time) {
				curve = map.get(ivTime);
				//System.out.println(ivTime);
				break;
			}
		}
		
		if (curve != null) {
			int index = curve.getIndex(current);
			double interCurrent = curve.getInterpolatedCurrent(index);
			double interVoltage = getInterpolatedVoltage(index);
			double m = interVoltage / interCurrent;
			retval =  m * (current - curve.getCurrent(index+1)) + voltages[index+1];
		} else {
			retval = -1;
			System.out.println("IV curve null at time = " + time);
		}
		
		return retval;
	}
	
	private double getInterpolatedVoltage (int index) {
		return voltages[index+1] - voltages[index];
	}
	
	public void setMinVoltage (double minvoltage) {
		this.minvoltage = minvoltage;
	}
	
	public void setMaxVoltage (double maxvoltage) {
		this.maxvoltage = maxvoltage;
	}
	
	public void setMinVoltage (String minvoltage) {
		this.minvoltage = Double.parseDouble(minvoltage);
	}
	
	public void setMaxVoltage (String maxvoltage) {
		this.maxvoltage = Double.parseDouble(maxvoltage);
	}
	
	public String toString() {
		String ret = "";
		for (Entry<?, ?> entry : map.entrySet()) {
			ret += entry.getKey() + " : ";
			ret += entry.getValue().toString() + "\n";
		}
		return ret;
	}
	
	public double getMaxVoltage () {
		return maxvoltage;
	}
}
