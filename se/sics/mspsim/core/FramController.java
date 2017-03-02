/*
 * Copyright (c) 2009, Friedrich-Alexander University Erlangen, Germany
 * 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
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
 * This file is part of mspsim.
 *
 */
/**
 * @author Klaus Stengel <siklsten@informatik.stud.uni-erlangen.de>
 * @author Víctor Ariño <victor.arino@tado.com>
 */
package se.sics.mspsim.core;

import java.util.Arrays;

import se.sics.mspsim.core.EmulationLogger.WarningType;
import se.sics.mspsim.core.Memory.AccessMode;
import se.sics.mspsim.util.Utils;
import edu.umass.energy.Capacitor;

public class FramController extends IOUnit {
  
  private static final int FRCTL0 = 0x00;
  private static final int GCCTL0 = 0x04;
  private static final int GCCTL1 = 0x06;

  /* Size of the fram controller */
  public static final int SIZE = 8;
  
  private static final int FRKEY = 0xa5;
  private static final int RESET_VECTOR = 15;
  private static final int CMDMASK = 0x00ff;

  /* FRCTL0 Control Bits */
  private static int frctlpw = 0x96;  // Initially
  
  /* GCCTL0 Control Bits */
  private static final int FRPWR = 0x4; // bit 3
  private static final int FRLPMPWR = 0x2; // bit 2
    
  /* Don't think these are necessary but I'll wait to remove them... */
  private FlashRange main_range;
  private FlashRange info_range;
  
  // This variable represents when the fram controller cannot be written to because the proper key hasn't been set
  private static int frampower;
  
  // This variable represents the active state of the fram
  private static boolean powerstate;
      
  public FramController(MSP430Core cpu, int[] memory, FlashRange main_range,
      FlashRange info_range, int offset) {
    super("Fram", "Internal Fram", cpu, memory, offset);
    this.main_range = main_range;
    this.info_range = info_range;
    
    Arrays.fill(memory, main_range.start, main_range.end, 0xff);
    Arrays.fill(memory, info_range.start, info_range.end, 0xff);

    reset(MSP430.RESET_POR);
  }

  public void interruptServiced(int vector) {
    cpu.flagInterrupt(vector, this, false);
  }
  
  public boolean addressInFram(int address) {
    if (main_range.isInRange(address)) {
      return true;
    }
    if (info_range.isInRange(address)) {
      return true;
    }
    
    return false;
  }
  
  private void setPassword(int value) {
	  if (value == FRKEY)
		  frctlpw = FRKEY;
	  else
	  	logw(WarningType.EXECUTION, "Bad key accessing flash controller");
	  	//statusreg |= KEYV;

	  // Trigger a reset if the key isn't correct and its currently locked still
	  if (frctlpw != FRKEY) {
	  	cpu.flagInterrupt(RESET_VECTOR, this, true);
	  	logw(WarningType.EXECUTION, "--> Flash controller reset");
	  }
	  frctlpw = value;
  }
  
  public void framWrite(int address, int data, AccessMode dataMode) {
	 
	  /* This is FRAM which simply writes a byte (Doesn't clear bits like flash...) */
      memory[address] = data & 0xff;
      if (dataMode != AccessMode.BYTE) {
          memory[address + 1] = (data >> 8) & 0xff;
          if (dataMode == AccessMode.WORD20) {
              /* TODO should the write really write the full word? CHECK THIS */
              memory[address + 2] = (data >> 16) & 0xff;
              memory[address + 3] = (data >> 24) & 0xff;
          }
      }
  }
  
  /* To be implemented if using the FRCTL's at top of code */
  public int read(int address, boolean word, long cycles) {
	  address = address - offset;
      
	  if (address == FRCTL0) {
		  //System.out.println("FRCTL0 = " + frctlpw);
		  return frctlpw;
	  }
	  if (address == GCCTL0) {
		  //System.out.println("GCCTL0 = " + frampower);
		  return frampower;
	  }
	  /*if (address == GCCTL1) {
		  System.out.println("GCCTL1");
	  } else {
		  System.out.println("Address = " + Utils.hex(address));
		  System.out.println("Offset = " + Utils.hex(offset));
	  }*/
    
	  return 0;
  }
  
  /* To be implemented if using the FRCTL's at top of code */
  public void write(int address, int value, boolean word, long cycles) {
	address = address - offset;
	
	// The address should always be one of these three registers
	if (!(address == FRCTL0 || address == GCCTL0 || address == GCCTL1 )) {
		System.err.println("Bad address in fram controller");
	    return;
	}

	int regdata = value & CMDMASK; // value & 0xff
		
	switch (address) {
		case FRCTL0:
			setPassword(regdata); // Set the password for the Fram Controller
			break;
		case GCCTL0:
			// check key will reset the fram if the key isn't unlocked
			if (frctlpw == FRKEY) {
				if ((frampower & FRPWR) != (regdata & FRPWR)) {
					frampower ^= FRPWR; // toggle the second bit
				}
				if ((frampower & FRLPMPWR) != (regdata & FRLPMPWR)) {
					frampower ^= FRLPMPWR; // toggle the third bit
				}
			} else {
				System.err.println("Couldn't write to GCCTL0 because FRCTL0 is locked\n");
			}
			break;
		// case GCCTL1: not needed for power settings
	}
	
	setPowerStateByPWRBit();
    
    cpu.isFlashBusy = false;
  }
  
  // Set the FRPWR bit low
  public void setFRPWRLOW()
  {
	  frampower &= ~FRPWR;
	  setPowerStateByPWRBit();
  }
  
  // Set the FRPWR bit high
  public void setFRPWRHigh()
  {
	  frampower |= FRPWR;
	  setPowerStateByPWRBit();
  }
  
  // This function sets the power state based on the status of the FRPWR bit
  public static void setPowerStateByPWRBit()
  {
	  // Always set the power state to true if FRPWR bit is high
	  powerstate = ((frampower & FRPWR) == FRPWR) ? true : false;
  }
  
  // This function sets the power state based on an arbitrary value and is used when moving from active to LPM
  public static void setPowerState(boolean state)
  {
	  powerstate = state;
  }
  
  public static boolean getPowerState()
  {
	  return powerstate;
  }
}
