package edu.clemson.platform;

/**
 * Simple Mayfly node with Remanence timekeeper support, on an FRAM enabled platform.
 * 
 * Will eventually have support for:
 * 		- UFoP: Federated Energy Storage
 * 		- Ultra low power wakeup radio
 * 		- CC1101 Transceiver
 * 		- Analog sensors: temperature, sunlight, leaf wetness, humidity
 * -----------------------------------------------------------------
 *
 * MayflyNode
 *
 * Author  : jhester@clemson.edu
 * Created : May 11 2016
 */

import java.io.IOException;

import se.sics.mspsim.chip.Decagon;
import se.sics.mspsim.chip.HDC1080;
import se.sics.mspsim.chip.TMP20;
import se.sics.mspsim.config.MSP430fr6989Config;
import se.sics.mspsim.core.ADC12_B;
import se.sics.mspsim.core.ADCInput;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.USARTSource;
// import se.sics.mspsim.extutil.jfreechart.DataChart;
// import se.sics.mspsim.extutil.jfreechart.DataSourceSampler;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.util.ArgumentManager;
import edu.clemson.time.RemanenceTimekeeper;

public class MayflyNode extends GenericNode implements ADCInput {
  public static final boolean DEBUG = false;
  public static final int MODE_MAX = 0; // ?
  
  private int xlData = 0;
  private TMP20 tempsensor;
  private Decagon wetnessSensor; 
  private HDC1080 humiditysensor;
  
  private String serialText;
  public MayflyNode () {
      super("Mayfly", new MSP430fr6989Config());
      this.serialText = "";
      super.timekeeper = new RemanenceTimekeeper( 
      		10e-8,  
      		20000000, 
      		1.0, 
      		1.8);
      tempsensor = new TMP20("TMP20", "Analog Temperature Sensor", cpu); // To get time for the temperature sensor
      wetnessSensor = new Decagon("Decagon", "Analog Leaf Wetness Sensor", cpu); // To get time for the leaf wetness sensor
      // have to declare humidity sensor later because it needs a USART source
  }

  public boolean getDebug () {
      return cpu.getDebug();
  }

  public void setDebug (boolean debug) {
      cpu.setDebug(debug);
  }

  public void setupNode () {
      
      // Setup the HumiditySensor
      IOUnit usart = cpu.getIOUnit("USCI B0");
      if (usart instanceof USARTSource) {
    	  USARTSource us = (USARTSource)usart;
       	  humiditysensor = new HDC1080("HDC1080", 0x40, us, cpu);
      } else {
    	  System.err.println("Couldn't set up humidity sensor!");
      }
      
      IOUnit adc = cpu.getIOUnit("ADC12_B");
      if (adc instanceof ADC12_B) {
    	// RemTim voltage
    	((ADC12_B) adc).setADCInput(0, tempsensor);
    	
    	((ADC12_B) adc).setADCInput(1, this);
    	((ADC12_B) adc).setADCInput(2, this);
    	((ADC12_B) adc).setADCInput(3, this);
    	//((ADC12_B) adc).setADCInput(4, tempsensor);
    	//((ADC12_B) adc).setADCInput(5, wetnessSensor);
      }
       
      // Connect the Timekeeper to Port 1 (BIT5)
      IOUnit port1 = cpu.getIOUnit(IOPort.class, "P1");
      if (port1 instanceof IOPort) {
    	  ((IOPort) port1).addPortListener(timekeeper);
      }
      
      // Connect the TemperatureSensor to Port 2 (BIT4)
      // Connect the LeafWetnessSensor to Port 2 (BIT 3)
      // Connect the HumiditySensor to Port 2 (BIT 2)
      IOUnit port2 = cpu.getIOUnit(IOPort.class, "P2");
      if (port2 instanceof IOPort) {
    	  ((IOPort) port2).addPortListener(tempsensor);
    	  ((IOPort) port2).addPortListener(wetnessSensor);
    	  ((IOPort) port2).addPortListener(humiditysensor);
      }
  }

  public String getName () {
      return "Mayfly";
  }

  public int getModeMax () {
      return MODE_MAX;
  }

  public static void main (String[] args) throws IOException {
      ArgumentManager config = new ArgumentManager();
      config.handleArguments(args);
      MayflyNode node;

      node = new MayflyNode();
      node.setupArgs(config);
      
      /*System.err.println("You're using Eclipse; click in this console and	" +
				"press Q to call System.exit() and run the shutdown routine.");
      try {
    	  int input = System.in.read();
      } catch (IOException e) {
    	  // TODO Auto-generated catch block
    	  e.printStackTrace();
      }
      System.exit(0);*/
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
}
