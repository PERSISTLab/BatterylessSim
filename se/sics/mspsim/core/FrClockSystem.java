/**
 * Copyright (c) 2007, Swedish Institute of Computer Science.
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
 * UnifiedClockSystem
 *
 * Author  : Joakim Eriksson
 * Author  : Adam Dunkels
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.core;
import se.sics.mspsim.util.Utils;

public class FrClockSystem extends ClockSystem {

  private static final int CSCTL0 = 0x0160;
  private static final int CSCTL1 = 0x0162;
  private static final int CSCTL2 = 0x0164;
  private static final int CSCTL3 = 0x0166;
  private static final int CSCTL4 = 0x0168;
  private static final int CSCTL5 = 0x016a;
  private static final int CSCTL6 = 0x016c;

  /* UCSCTL0 Control Bits */
  private static final int CSKEY_BITPOS = 8;
  private static final int CSKEY_BITWIDTH = 8;
  
  /* UCSCTL1 Control Bits */
  private static final int DCORSEL_BITPOS = 6;  
  private static final int DCOFSEL_BITPOS = 1;
  private static final int DCOFSEL_BITWIDTH = 3;
  
  /* UCSCTL3 Control Bits */
  private static final int SMCLK_BITPOS = 4;
  private static final int SMCLK_BITWIDTH = 3;
  
  private static final int ACLK_FRQ = 32768;
  private static final int MAX_DCO_FRQ = 16000000;
  private static final int CSKEY = 0xa5;
  
  private final Timer[] timers;

  private int currentDcoFrequency;

  /**
   * Creates a new <code>FrClockSystem</code> instance.
   *
   */
  public FrClockSystem(MSP430Core cpu, int[] memory, int offset, Timer[] timers) {
    super("FrClockSystem", cpu, memory, offset);
    this.timers = timers;
  }

  public int getMaxDCOFrequency() {
      return MAX_DCO_FRQ;
  }

  public int getAddressRangeMin() {
    return CSCTL0;
  }

  public int getAddressRangeMax() {
    return CSCTL6;
  }

  public void reset(int type) {
    // Set the reset states, according to the slau367i data sheet.
    write(CSCTL0, 0x9600, true, cpu.cycles);
    write(CSCTL1, 0x000c, true, cpu.cycles);
    write(CSCTL2, 0x0033, true, cpu.cycles);
    write(CSCTL3, 0x0033, true, cpu.cycles);
    write(CSCTL4, 0xcdc9, true, cpu.cycles);
    write(CSCTL5, 0x00c0, true, cpu.cycles);
    write(CSCTL6, 0x0007, true, cpu.cycles);
  }

  // do nothing?
  public int read(int address, boolean word, long cycles) {
    int val = memory[address];
    if (word) {
      val |= memory[(address + 1) & 0xffff] << 8;
    }
    return val;
  }

  public void write(int address, int data, boolean word, long cycles) {
    if (DEBUG) log("Write to MattsClockSystem: " +
		       Utils.hex16(address) + " => " + Utils.hex16(data));

    memory[address] = data & 0xff;
    if (word) memory[address + 1] = (data >> 8) & 0xff;

    if (address == CSCTL1)// Similar to BasicClockModule
    	updateTimers(cycles);
    
    setConfiguration(cycles);
  }

  public void interruptServiced(int vector) {
  }


  private void setConfiguration(long cycles) {
    // Read a configuration from the UCSCTL* registers and compute the timer setup
	int dcoFreq = 0;
	
    // Get cskey to see if clock system is unlocked
    int cskey = (read(CSCTL0, true, cycles) >> CSKEY_BITPOS) & ((1 << CSKEY_BITWIDTH) - 1);

    // Get DCO range select from CSCTL1
    int dcoRange = (read(CSCTL1, true, cycles) >> DCORSEL_BITPOS) & 1;
    
    // Get DCO frequency select from CSCTL1
    int dcoSel = (read(CSCTL1, true, cycles) >> DCOFSEL_BITPOS) & ((1 << DCOFSEL_BITWIDTH) - 1);

    /*
     * 	The chart below is used for the switch statement below it
     * 
     * 	000b = If DCORSEL = 0: 1 MHz; If DCORSEL = 1: 1 MHz
	 * 	001b = If DCORSEL = 0: 2.67 MHz; If DCORSEL = 1: 5.33 MHz
	 *	010b = If DCORSEL = 0: 3.33 MHz; If DCORSEL = 1: 6.67 MHz
	 *	011b = If DCORSEL = 0: 4 MHz; If DCORSEL = 1: 8 MHz
	 *	100b = If DCORSEL = 0: 5.33 MHz; If DCORSEL = 1: 16 MHz
	 *	101b = If DCORSEL = 0: 6.67 MHz; If DCORSEL = 1: 21 MHz
	 *	110b = If DCORSEL = 0: 8 MHz; If DCORSEL = 1: 24 MHz
	 *	111b = If DCORSEL = 0: Reserved. Defaults to 8 MHz. It is not recommended to
	 *	use this setting; If DCORSEL = 1: Reserved. Defaults to 24 MHz. It is not
	 *	recommended to use this setting
     */
    
    switch (dcoSel) { // All values in MHz
    	case 0:
    		dcoFreq = 100;	
    		break;
    	case 1:
    		dcoFreq = 267;
    		if (dcoRange == 1)
    			dcoFreq *= 2;
    		break;
    	case 2:
    		dcoFreq = 333;
    		if (dcoRange == 1)
    			dcoFreq *= 2;
    		break;
    	case 3:
    		dcoFreq = 400;
    		if (dcoRange == 1)
    			dcoFreq *= 2;
    		break;
    	case 4:
    		dcoFreq = 533;
    		if (dcoRange == 1)
    			dcoFreq *= 3;
    		break;
    	case 5:
    		dcoFreq = 667;
    		if (dcoRange == 1)
    			dcoFreq *= 3;
    		break;
    	case 6:
    		dcoFreq = 800;
    		if (dcoRange == 1)
    			dcoFreq *= 3;
    		break;
    	case 7:
    		dcoFreq = 800;
    		if (dcoRange == 1)
    			dcoFreq *= 3;
    		break;
    }
    
    // Get smclk divisor from CSCTL3
    int divSMclk = (read(CSCTL3, true, cycles) >> SMCLK_BITPOS) & ((1 << SMCLK_BITWIDTH) - 1);
    
    int newDcoFrequency = (dcoFreq + 1) * ACLK_FRQ;

    if (cskey != CSKEY)
    	log("The clock system key does not match!");
    
    // Double check the cskey is unlocked before changing frequency!
    if (newDcoFrequency != currentDcoFrequency && cskey == CSKEY) {
      currentDcoFrequency = newDcoFrequency;
      cpu.setDCOFrq(currentDcoFrequency, currentDcoFrequency / (1 << divSMclk));

      updateTimers(cycles);
    }
  }
  
  private void updateTimers(long cycles) {
	  if (timers != null) {
		  for(int i = 0; i < timers.length; i++) {
			  timers[i].resetCounter(cycles);
	      }
	  }
  }
}
