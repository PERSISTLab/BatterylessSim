
package se.sics.mspsim.platform.ti;
import java.io.IOException;

import se.sics.mspsim.config.MSP430fr6989Config;
import se.sics.mspsim.core.ADC12;
import se.sics.mspsim.core.ADCInput;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.USARTListener;
import se.sics.mspsim.core.USARTSource;
// import se.sics.mspsim.extutil.jfreechart.DataChart;
// import se.sics.mspsim.extutil.jfreechart.DataSourceSampler;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.platform.crfid.MooNode;
import se.sics.mspsim.util.ArgumentManager;

public class Exp6989Node extends GenericNode implements ADCInput, USARTListener {
  public static final boolean DEBUG = false;
  public static final int MODE_MAX = 0; // ?

  private String serialText;
  public Exp6989Node () {
      super("Exp6989Node", new MSP430fr6989Config());
      this.serialText = "";
  }

  public boolean getDebug () {
      return cpu.getDebug();
  }

  public void setDebug (boolean debug) {
      cpu.setDebug(debug);
  }

  public void setupNode () {
      IOUnit adc = cpu.getIOUnit("ADC12");
      if (adc instanceof ADC12) {
        ((ADC12) adc).setADCInput(0, this);
      }
      
      // Add some windows for listening to serial output
      IOUnit usart = cpu.getIOUnit("USCI A0");
      if (usart instanceof USARTSource) {
    	  USARTSource us = (USARTSource)usart;
    	  us.addUSARTListener(this);
      }
  }

  public String getName () {
      return "Exp6989";
  }

  public int getModeMax () {
      return MODE_MAX;
  }

  public static void main (String[] args) throws IOException {
      ArgumentManager config = new ArgumentManager();
      config.handleArguments(args);
      Exp6989Node node;

      node = new Exp6989Node();
      node.setupArgs(config);
  }

  public void exitCleanup () {
      System.err.println("exitCleanup() called");
      System.err.println("Final voltage: " + cpu.getCapacitor().getVoltage());
      System.err.println("Number of lifecycles: " +
              cpu.getCapacitor().getNumLifecycles());
  }

  public int nextData(int adcPos) {
	  // sinusoid w/ 500 Hz frequency
	  double time_ms = cpu.getTimeMillis();
	  double sinval = Math.sin(time_ms * Math.PI);
	  return (int)(sinval * 127); // sinusoid within range of byte
  }

  @Override
  public void dataReceived(USARTSource source, int data) {
	 if (((char)data) == '\n') {
		 System.out.println("printf: "+serialText);
		 serialText = "";
	 } else {
		 serialText += (char)data;
	 }
  }
}
