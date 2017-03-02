package se.sics.mspsim.platform;

public class ShouldRetryLifecycleException extends Exception {
	private static final long serialVersionUID = 570632615085906315L;
	private double ova = 0.0;
	
	public ShouldRetryLifecycleException (String msg,
			double oracleVoltageAdjustment) {
		super(msg);
		ova = oracleVoltageAdjustment;
	}
	
	public double getOracleVoltageAdjustment () {
		return ova;
	}
}
