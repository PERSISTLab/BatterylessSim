package edu.clemson.time;


import se.sics.mspsim.core.ADCInput;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.PortListener;

public class RemanenceTimekeeper implements ADCInput, PortListener {

	private double capacitance;
	private double effectiveMaxVoltage;
	private double voltage;
	private double resistance;
	private static final int TIMEKEEPER_POWER_BIT = 0x20;


	public RemanenceTimekeeper (double C, double R,
			double inputVoltageDividerFactor,
			double inputVoltageReferenceVoltage) {
		capacitance = C;
		resistance = R;
        effectiveMaxVoltage = (inputVoltageDividerFactor * inputVoltageReferenceVoltage);
        voltage = effectiveMaxVoltage;
	}

	public int nextData(int adcPos) {
		int adc_val = (int)Math.round((voltage / effectiveMaxVoltage) * 4095.0);
		return adc_val;
	}
	
	protected void setVoltage(double V) {
		voltage = V;
	}
	
	public double getVoltage() {
		return voltage;
	}
	
	/** 
	 * 
	 * @param dt
	 */
	public void updateVoltage(double dt) {
			double RC = resistance * capacitance;
			setVoltage(
					voltage * // initial condition
					Math.exp(
							(-1.0 * dt) // time
							/ (800.0 * RC)
							)
					);
		
	}

	@Override
	public void portWrite(IOPort source, int data) {
		// If direction is output
		if((source.getDirection() & TIMEKEEPER_POWER_BIT) != 0) {
			if((source.getOut() & TIMEKEEPER_POWER_BIT) != 0) {
				// HIGH
				setVoltage(effectiveMaxVoltage);
			} else {
			}
		}
	}
}
