package edu.clemson.time;

public class CapacitorTest {

	public static void main(String[] args) {
		double C = 10e-8; 
		double R = 20000000;
		double RC = R * C;
		double startVoltage = 1.8;
		// Time in milliseconds
		for (int dt = 0; dt < 8192; dt+=128) {
			double V = startVoltage *
					Math.exp(
							(-1.0 * dt) // time
							/ (800.0 * RC)
							);
			int adc_val = (int)Math.round((V / startVoltage) * 4095.0);
			System.out.printf("%d,%d\n", dt,adc_val);
		} 
	}

}
