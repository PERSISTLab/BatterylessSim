/**
 * Copyright (c) 2012, University of Massachusetts.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of MSPSim.
 *
 * $Id$
 *
 * -----------------------------------------------------------------
 *
 * MooNode
 *
 * Author  : ransford@cs.umass.edu
 * Created : Fri Nov 13 10:27:50 2009
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.platform.crfid;
import java.io.IOException;

import edu.clemson.time.RemanenceTimekeeper;
import se.sics.mspsim.config.MSP430f2618Config;
import se.sics.mspsim.core.ADC12;
import se.sics.mspsim.core.ADCInput;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.USARTListener;
import se.sics.mspsim.core.USARTSource;
// import se.sics.mspsim.extutil.jfreechart.DataChart;
// import se.sics.mspsim.extutil.jfreechart.DataSourceSampler;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.util.ArgumentManager;

public class MooNode extends GenericNode implements ADCInput, USARTListener {
  public static final boolean DEBUG = false;
  public static final int MODE_MAX = 0; // ?
  
  private String serialText;
  public MooNode () {
      super("Moo", new MSP430f2618Config());
      this.serialText = "";
      super.timekeeper = new RemanenceTimekeeper( 
      		10e-8,  
      		20000000, 
      		1.0, 
      		1.8);
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
        // RemTim voltage
        ((ADC12) adc).setADCInput(7, timekeeper);
      }
       
      // Connect the Timekeeper to Port 1 (BIT5)
      IOUnit port1 = cpu.getIOUnit(IOPort.class, "P1");
      if (port1 instanceof IOPort) {
    	  ((IOPort) port1).addPortListener(timekeeper);
      }
      
      // Show serial output from node
      IOUnit usart = cpu.getIOUnit("USCI A0");
      if (usart instanceof USARTSource) {
    	  USARTSource us = (USARTSource)usart;
    	  us.addUSARTListener(this);
      }
  }

  public String getName () {
      return "Moo";
  }

  public int getModeMax () {
      return MODE_MAX;
  }

  public static void main (String[] args) throws IOException {
      ArgumentManager config = new ArgumentManager();
      config.handleArguments(args);
      MooNode node;

      node = new MooNode();
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
	  return (int)((sinval+1.0) * 2047); // sinusoid within range of 12-bit adc / 2
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
