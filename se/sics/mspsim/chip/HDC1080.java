package se.sics.mspsim.chip;

import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.core.USARTSource;
import edu.umass.energy.Capacitor;

/*
 * We will assume for this sensor that measurements are being taken once per second
 */
public class HDC1080 extends I2CUnit implements PortListener {

	private int temperature; // in degrees celsius	
	private double humidity; // percentage between 0 and 100
	private int config = 0x02; // default config
	
	private Capacitor c;
	private MSP430Core cpu;
	private static final int HDC_POWER_BIT = 0x4;
	
	public static final int TEMP_REG = 0;
	public static final int HUMIDITY_REG = 1;
	public static final int CONFIG_REG = 2;
	
	private static final int STARTUP_TIME = 10; // in milliseconds
	private static final int POLLING_RATE = 1000; // in milliseconds
	private static final int MAX_HUMIDITY = 100; // 100 percent
	private static final int MIN_HUMIDITY = 0; // 0 percent
	
	private boolean rst = false;
	private boolean heat = false;
	private boolean mode = false;
	private boolean afterStartup = false;
	private int tres = 0x1;
	private int hres = 0x3;
	
	private double lastReadTime;
			
	public HDC1080(String name, int address, USARTSource src, MSP430Core cpu) {
		super("hdc1080", 0x40, src, cpu); // The I2C address of the HDC1080 is 1000000 (7-bit address).
		this.cpu = cpu;
		c = cpu.getCapacitor();
		c.addPeripheral(this, 30e-6); // current draw is 30 microA on startup
		lastReadTime = cpu.getTimeMillis();
		temperature = randomTemperature();
		humidity = randomHumidity();
	}

	@Override
	protected void registerWrite(int address, int value) {
		switch (address) {
			case CONFIG_REG:
				// System.out.println("Write to CONFIG_REG with value = " + value);
				config = value;
				/* Parse some of the configurations */
				rst = ((value & 0x8000) > 0); // Bit 15
				heat = ((value & 0x2000) > 0); // Bit 13
				mode = ((value & 0x1000) > 0); // Bit 12
				// btst = ((value & 0x800) > 0); // Bit 11 (READ ONLY)
				tres = ((value & 0x400) >> 10); // Bit 10
				hres = ((value & 0x300) >> 8); // Bits 8 & 9
				updateCurrent();
				break;
			case TEMP_REG:
				logw("Temperature Register is Read-Only!");
				break;
			case HUMIDITY_REG:
				logw("Humidity Register is Read-Only!");
				break;
			default:
				logw("not implemented");
				break;
		}
	}

	@Override
	protected int registerRead(int address) {
		if (cpu.getTimeMillis() - lastReadTime >= STARTUP_TIME && !afterStartup) { // if after startup time
			afterStartup = true;
			lastReadTime = cpu.getTimeMillis();
		}
		switch (address) {
			case TEMP_REG:
				if (cpu.getTimeMillis() - lastReadTime >= POLLING_RATE && afterStartup) { // Must be one second since the last reading and past startup
					// System.out.println("Read = TEMP_REG");
					lastReadTime = cpu.getTimeMillis();
					return getRawTemperature() & 0xffff; // Not confident about this...
				} 
				break;
			case HUMIDITY_REG:
				if (cpu.getTimeMillis() - lastReadTime >= POLLING_RATE && afterStartup) { // Must be one second since the last reading and past startup
				//	System.out.println("Read = HUMIDITY_REG");
					lastReadTime = cpu.getTimeMillis();
					return (int) getRawHumidity() & 0xffff; // Not confident about this...
				} 
				break;
			case CONFIG_REG:
				System.out.println("Read = CONFIG_REG");
				return config;
			default:
				logw("Not yet implemented");
				break;
		}
		return -1;
	}
	
	/*
	 *	Temperature(C) = (TEMP_REG[15:00] / 2^16) * 165C - 40C
	 *	or
	 *	TEMP_REG[15:00] = (Temperature(C) + 40) / 165 * 2^16
	 */
	private synchronized int getRawTemperature() {
		// sinusoid w/ 500 Hz frequency
		double time_ms = cpu.getTimeMillis();
		double sinval = Math.sin(time_ms * Math.PI);
		return (int)(sinval * 127); // sinusoid within range of byte
	}
	
	private int randomTemperature ()
	{
		return (int) (Math.random() * 116) + 15; // This should be a temperature between 15 and 130
	}

	private synchronized int getRawHumidity() {
		// sinusoid w/ 500 Hz frequency
		double time_ms = cpu.getTimeMillis();
		double sinval = Math.sin(time_ms * Math.PI);
		return (int)(sinval * 127); // sinusoid within range of byte
	}
	
	private int randomHumidity ()
	{
		return (int) (Math.random() * MAX_HUMIDITY); // This should be a humidity between 0 and 100
	}

	public double getHumidity() {
		return humidity;
	}

	public int getConfig() {
		return config;
	}

	public boolean isRst() {
		return rst;
	}

	public boolean isHeat() {
		return heat;
	}

	public boolean isMode() {
		return mode;
	}

	/*public boolean isBtst() {
		return btst;
	}*/

	public int getTres() {
		return tres;
	}

	public int getHres() {
		return hres;
	}
	
	public void updateCurrent () {
		double current = 0.0;
		
		if (heat && mode) { // Heater on and temperature and humidity measurement recorded
			current += 50e-6; // Average @ 1 measurement/second
		} else if (!mode) { // Humidity measurement but not temperature measurement
			current += 710e-9; // Average @ 1 measurement/second
		} else if (mode) { // Humidity measurement and temperature measurement
			current += 1.3e-6; // Average @ 1 measurement/second
		}
		
		// Change the current value of this peripheral
		c.changePeripheralCurrent(this, current);
	}

	@Override
	public void portWrite(IOPort source, int data) {
		if (setCurrentDraw(source, HDC_POWER_BIT) == Peripheral.POWER_ON)
			lastReadTime = cpu.getTimeMillis();
	}
}
