package se.sics.mspsim.chip;

import se.sics.mspsim.core.ADCInput;
import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.PortListener;
import edu.umass.energy.Capacitor;


public class TMP20 extends Chip implements TemperatureChip, ADCInput, PortListener {

	private int temperature; // in degrees celsius
	private Capacitor c;
	
	private static final int TMP_POWER_BIT = 0x10;
	
	private static final double STARTUP_TIME = .2; // 200 microseconds in milliseconds
	
	private double lastReadTime;
	private boolean afterStartup = false;
	
	public TMP20(String id, String name, MSP430Core cpu) 
	{
		super(id, name, cpu);
		temperature = randomTemperature();
		c = cpu.getCapacitor();
		c.addPeripheral(this, 4e-6); // current draw is 4microA
	}
	
	public void setTemperature(int temp) {
		this.temperature = temp;
	}

	@Override
	public int getMaxTemperature() {
		return 130;
	}

	@Override
	public int getMinTemperature() {
		return 15;
	}

	@Override
	public void portWrite(IOPort source, int data) {
		if (setCurrentDraw(source, TMP_POWER_BIT) == Peripheral.POWER_ON)
			lastReadTime = cpu.getTimeMillis();
	}

	@Override
	public int getTemperature() {
		return temperature;
	}

	@Override
	public int nextData(int adcPos) {
		
		
		if (cpu.getTimeMillis() - lastReadTime >= STARTUP_TIME && !afterStartup) { // if after startup time
			afterStartup = true;
			lastReadTime = cpu.getTimeMillis();
		}
		
		if (afterStartup) {
			// sinusoid w/ 500 Hz frequency
			double time_ms = cpu.getTimeMillis();
			double sinval = Math.sin(time_ms * Math.PI);
			return (int)(sinval * 127); // sinusoid within range of byte
		}
		
		return -1;
	}
	
	private int randomTemperature ()
	{
		return (int) (Math.random() * 116) + 15; // This should be a temperature between 15 and 130
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
