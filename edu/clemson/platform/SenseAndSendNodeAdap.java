package edu.clemson.platform;

import java.io.IOException;

import se.sics.mspsim.chip.CC1101;
import se.sics.mspsim.chip.PacketListener;
import se.sics.mspsim.chip.TMP20;
import se.sics.mspsim.config.MSP430fr6989Config;
import se.sics.mspsim.core.ADC12_B;
import se.sics.mspsim.core.ADCInput;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.core.USARTListener;
import se.sics.mspsim.core.USARTSource;
// import se.sics.mspsim.extutil.jfreechart.DataChart;
// import se.sics.mspsim.extutil.jfreechart.DataSourceSampler;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.platform.sky.RadioWrapper;
import se.sics.mspsim.util.ArgumentManager;
import se.sics.mspsim.util.NetworkConnection;
import se.sics.mspsim.util.Utils;
import edu.clemson.time.RemanenceTimekeeper;

public class SenseAndSendNodeAdap extends GenericNode implements ADCInput, PortListener, USARTListener {

	/* P1.3 - Output: SPI Chip Select (CS_N) */
	/* P1.6 - Output: SPI Slave IN from CC2520 */
	/* P1.7 - Input: SPI Slave OUT from CC2520 */
	public static final int CC2520_CHIP_SELECT = (1 << 3);
	public static final int CC2520_SI = (1 << 6);
	public static final int CC2520_SO = (1 << 7);


	/* P2.2 - Output: SCLK to CC2520 */
	/* P2.5 - Input: GDO0 from CC2520 */
	public static final int CC2520_SCLK = 2;
	public static final int CC2520_GDO0 = 5;

	/* P3.6 - Input: GDO2 from CC2520 */
	public static final int CC2520_GDO2 = 6;

	protected IOPort port1;
	protected IOPort port2;
	protected IOPort port3;
	
	public static final boolean DEBUG = false;
	public static final int MODE_MAX = 0; // ?

	private int xlData = 0;
	private CC1101 radio;

	
	private TMP20 tempsensor;
	
	public SenseAndSendNodeAdap () {
		super("SENSEANDSENDSEMI", new MSP430fr6989Config());
		super.timekeeper = new RemanenceTimekeeper( 
				10e-8,  
				20000000, 
				1.0, 
				1.8);
		tempsensor = new TMP20("TMP20", "Analog Temperature Sensor", cpu); // To get time for the temperature sensor
	}

	public boolean getDebug () {
		return cpu.getDebug();
	}

	public void setDebug (boolean debug) {
		cpu.setDebug(debug);
	}
	
	public void setupNodePorts() {
        port1 = cpu.getIOUnit(IOPort.class, "P1");
        port1.addPortListener(this);

        port2 = cpu.getIOUnit(IOPort.class, "P2");
        port2.addPortListener(this);

        port3 = cpu.getIOUnit(IOPort.class, "P3");
        port3.addPortListener(this);

        IOUnit uscib0 = cpu.getIOUnit("USCI B0");
        radio = new CC1101("CC1101", "Radio", cpu); // Initialize radio
        radio.setGDO0Port(port2, CC2520_GDO0);
        radio.setSCLKPort(port2, CC2520_SCLK);
        radio.setGDO2Port(port3, CC2520_GDO2);

        if (uscib0 instanceof USARTSource) {
        	USARTSource us = (USARTSource)uscib0;
        	us.addUSARTListener(this);
        }
        
        IOUnit adc = cpu.getIOUnit("ADC12_B");
        if (adc instanceof ADC12_B) {
	      	// RemTim voltage
	        ((ADC12_B) adc).setADCInput(0, tempsensor);
        	((ADC12_B) adc).setADCInput(4, cpu.getCapacitor());
	      	
	      	((ADC12_B) adc).setADCInput(1, this);
	      	((ADC12_B) adc).setADCInput(2, this);
	      	((ADC12_B) adc).setADCInput(3, this);
        }
        
        // Connect the TemperatureSensor to Port 4 (BIT4)
        IOUnit port4 = cpu.getIOUnit(IOPort.class, "P4");
        if (port4 instanceof IOPort) {
      	  ((IOPort) port4).addPortListener(tempsensor);
          //((IOPort) port4).addPortListener(cpu.getCapacitor());
        }
    }


	public void setupNode () {
		setupNodePorts();
		
//		if (config.getPropertyAsBoolean("enableNetwork", false)) {
			final NetworkConnection network = new NetworkConnection();
			final RadioWrapper radioWrapper = new CC1101RadioWrapper(radio);
			radioWrapper.addPacketListener(new PacketListener() {
				public void transmissionStarted() {
					//System.out.println("Transmission started!");
				}
				public void transmissionEnded(byte[] receivedData) {
					                    /*System.out.println("**** Sending data len = " + receivedData.length);
					                    for (int i = 0; i < receivedData.length; i++) {
					                        System.out.println("Byte: " + Utils.hex8(receivedData[i]));
					                    }*/
					network.dataSent(receivedData);
				}
			});

			network.addPacketListener(new PacketListener() {
				public void transmissionStarted() {
				}
				public void transmissionEnded(byte[] receivedData) {
					                    System.out.println("**** Receiving data = " + receivedData.length);
					radioWrapper.packetReceived(receivedData);
				}
			});
		//}
		
		
	}

	public String getName () {
		return "Mayfly";
	}

	public int getModeMax () {
		return MODE_MAX;
	}

	public void exitCleanup () {
		System.err.println("exitCleanup() called");
		System.err.println("Final voltage: " + cpu.getCapacitor().getVoltage());
		System.err.println("Number of lifecycles: " +
				cpu.getCapacitor().getNumLifecycles());
	}

	public int nextData(int adcPos) {
		if(adcPos == 0) {
			xlData = 100;
		}
		if(adcPos == 1) {
			xlData = 200;
		}
		if(adcPos == 2) {
			xlData = 300;
		}
		return xlData;
	}

	@Override
	public void dataReceived(USARTSource source, int data) {
		radio.dataReceived(source, data);
	}

	@Override
	public void portWrite(IOPort source, int data) {
		if (source == port1) {
            // Chip select = active low...
			if((source.getDirection() & CC2520_CHIP_SELECT) != 0) {
				radio.setChipSelect((source.getOut() & CC2520_CHIP_SELECT) != 0);
			}
        } 
	}
}

