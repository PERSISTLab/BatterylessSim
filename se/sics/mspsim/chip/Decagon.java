package se.sics.mspsim.chip;

import edu.umass.energy.Capacitor;
import se.sics.mspsim.core.ADCInput;
import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.PortListener;

public class Decagon extends Chip implements ADCInput, PortListener {

	private int leafwetness;
	private Capacitor c;
	
	private static final int DEC_POWER_BIT = 0x08;
	
	public Decagon(String id, String name, MSP430Core cpu) {
		super(id, name, cpu);
		leafwetness = 100; // not sure what leaf wetness is measured in...
		c = cpu.getCapacitor();
		c.addPeripheral(this, 1e-3); // Current draw is 1mA
	}
	
	public void setLeafWetness (int leafwetness) {
		this.leafwetness = leafwetness;
	}
	
	public int getLeafWetness () {
		return leafwetness;
	}

	@Override
	public int nextData(int adcPos) {
		// sinusoid w/ 500 Hz frequency
		double time_ms = cpu.getTimeMillis();
		double sinval = Math.sin(time_ms * Math.PI);
		return (int)(sinval * 127); // sinusoid within range of byte
	}

	@Override
	public void portWrite(IOPort source, int data) {
		setCurrentDraw(source, DEC_POWER_BIT);
	}
	

	@Override
	public int getConfiguration(int parameter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getModeMax() {
		// TODO Auto-generated method stub
		return 0;
	}
}
