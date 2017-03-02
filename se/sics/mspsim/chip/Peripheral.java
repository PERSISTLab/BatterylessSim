package se.sics.mspsim.chip;

import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430Core;
import edu.clemson.eval.EvalLogger;
import edu.umass.energy.Capacitor;

public abstract class Peripheral {
	
	private MSP430Core cpu;
	public static final boolean POWER_ON = true;
	public static final boolean POWER_OFF = false;
	protected String name;
	
	public Peripheral (MSP430Core cpu) {
		this.cpu = cpu;
	}

	public String getName() {
		return name;
	}
	
	public boolean setCurrentDraw (IOPort source, int powerbit) {
		Capacitor c = cpu.getCapacitor();
		// If direction is output
		if((source.getDirection() & powerbit) != 0) {
			if((source.getOut() & powerbit) != 0) {
				// Minus current draw of temperature sensor 
				c.setPeripheralHigh(this);
				return POWER_ON;
			} else {
				// Plus current draw of temperature sensor
				c.setPeripheralLow(this);
			}
		}
		return POWER_OFF;
	}
}
