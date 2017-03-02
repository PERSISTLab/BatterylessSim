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
 * -----------------------------------------------------------------
 *
 * ADC12
 *
 * Each time a sample is converted the ADC12 system will check for EOS flag
 * and if not set it just continues with the next conversion (x + 1). 
 * If EOS next conversion is startMem.
 * Interrupt is triggered when the IE flag are set! 
 *
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 */

/* Copyright (c) 2013, tado° GmbH. Munich, Germany.
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
 * This file is part of MSPSim.
 * 
 * Author: Víctor Ariño <victor.arino@tado.com>
 * 
 */

package se.sics.mspsim.core;

import java.util.Arrays;

import se.sics.mspsim.core.EmulationLogger.WarningType;


/**
 * ADC12_B
 * 
 * This module extends the functionality of the ADC12Plus by adding more channels, like the ADC12_B.
 * Also changes the register addresses and offsets, as well as slightly different way of managing
 * interrupts and channels.
 * 
 * Does not yet support comparator or reference setting, does not support resolution changing.
 * 
 * The rest of functionalities are the same as the standard ADC12Plus
 * 
 * @author Josiah Hester <jhester@clemson.edu>
 * @author Joakim Eriksson <joakime@sics.se>
 * @author Víctor Ariño <victor.arino@tado.com>
 */
public class ADC12_B extends IOUnit {

	/**
	 * Address and size for IO configuration
	 */
	public static int OFFSET = 0x0800;
	public static int SIZE = 0x9f;

	public static final int ADC12CTL0 = 0x00;// Reset with POR
	public static final int ADC12CTL1 = 0x02;// Reset with POR
	public static final int ADC12CTL2 = 0x04;// Reset with POR XXX
	public static final int ADC12CTL3 = 0x06;// Reset with POR XXX

	public static final int ADC12IFGR0 = 0x0c;
	public static final int ADC12IFGR1 = 0x0e;
	public static final int ADC12IFGR2 = 0x10;
	public static final int ADC12IER0 = 0x12;
	public static final int ADC12IER1 = 0x14;
	public static final int ADC12IER2 = 0x16;
	public static final int ADC12IV = 0x18; // Reset with POR

	public static final int ADC12MEM0 = 0x60; // Unchanged
	public static final int ADC12MEM15 = 0x7E; // Unchanged
	public static final int ADC12MEM16 = 0x80; // Unchanged
	public static final int ADC12MEM31 = 0x9E; // Unchanged

	public static final int ADC12MCTL0 = 0x20; // Reset with POR
	public static final int ADC12MCTL31 = 0x5e; // Reset with POR

	public static final int[] SHTBITS = new int[] { 4, 8, 16, 32, 64, 96, 128,
			192, 256, 384, 512, 768, 1024, 1024, 1024, 1024 };

	public static final int BUSY_MASK = 0x01;
	public static final int EOS_MASK = 0x80;

	public static final int CONSEQ_SINGLE = 0x00;
	public static final int CONSEQ_SEQUENCE = 0x01;
	public static final int CONSEQ_REPEAT_SINGLE = 0x02;
	public static final int CONSEQ_REPEAT_SEQUENCE = 0x03;
	public static final int CONSEQ_SEQUENCE_MASK = 0x01;

	private int adc12ctl0 = 0;
	private int adc12ctl1 = 0;
	private int adc12ctl2 = 0;
	private int[] adc12mctl = new int[32];
	private int[] adc12mem = new int[32];
	private int adc12Pos = 0;

	private int shTime0 = 4;
	private int shTime1 = 4;
	private boolean adc12On = false;
	private boolean enableConversion;
	private boolean startConversion;
	private boolean isConverting;

	private int shSource = 0;
	private int startMem = 0;
	private int adcDiv = 1;

	private ADCInput adcInput[] = new ADCInput[32];

	private int conSeq;
	
	private int adc12ifgr0;
	private int adc12ifgr1;
	private int adc12ifgr2;
	
	private int adc12ier0;
	private int adc12ier1;
	private int adc12ier2;
	
	private int adc12iv;

	private int adcSSel;
	private int adc12Vector = 0x18;

	private TimeEvent adcTrigger = new TimeEvent(0) {
		public void execute(long t) {
			// System.out.println(getName() + " **** executing update timers at " +
			// t + " cycles=" + cpu.cycles);
			convert();
		}
	};

	/* These are CTL2 variables */
	private int bitsResolution = 12;
	private boolean formatSigned = false;
	private int clockPredivider = 1;

	/* Reference voltage 2.5V or 1.5V */
	private boolean ref25V = false;

	public ADC12_B(MSP430Core cpu) {
		super("ADC12_B", cpu, cpu.memory, OFFSET);
	}

	public void reset(int type) {
		enableConversion = false;
		startConversion = false;
		isConverting = false;
		adc12ctl0 = 0;
		adc12ctl1 = 0;
		adc12ctl2 = 0;
		shTime0 = shTime1 = 4;
		adc12On = false;
		shSource = 0;
		startMem = adc12Pos = 0;
		adcDiv = 1;

		conSeq = 0;
		adc12iv = 0;
		adcSSel = 0;
		
		adc12ifgr0 = 0;
		adc12ifgr1 = 0;
		adc12ifgr2 = 0;
		
		adc12ier0 = 0;
		adc12ier1 = 0;
		adc12ier2 = 0;

		Arrays.fill(adc12mctl, 0);

		clockPredivider = 1;
		formatSigned = false;
		bitsResolution = 12;
		ref25V = false;
	}

	public void setADCInput(int adindex, ADCInput input) {
		adcInput[adindex] = input;
	}

	/**
	 * Get the maximum input voltage set by the configuration of the registers
	 * (in mV)
	 * 
	 * @return
	 */
	public int getMaxInVoltage() {
		// TODO
		return 1800;
	}

	/**
	 * Get the minimum in voltage set by the registers (in mV)
	 * 
	 * @return
	 */
	public int getMinInVoltage() {
		// TODO
		return 0;
	}

	// write a value to the IO unit
	public void write(int address, int value, boolean word, long cycles) {
		address -= offset;
		switch (address) {
		case ADC12CTL0:
			if (enableConversion) {
				// Ongoing conversion: only some parts may be changed
				adc12ctl0 = (adc12ctl0 & 0xfff0) + (value & 0xf);
			} else {
				adc12ctl0 = value;
				shTime0 = SHTBITS[(value >> 8) & 0x0f];
				shTime1 = SHTBITS[(value >> 12) & 0x0f];
				adc12On = (value & 0x10) > 0;
			}
			enableConversion = (value & 0x02) > 0;
			startConversion = (value & 0x01) > 0;
			ref25V = (value & 0x20) > 0;

			if (DEBUG)
				log("Set SHTime0: " + shTime0 + " SHTime1: " + shTime1 + " ENC:"
						+ enableConversion + " Start: " + startConversion
						+ " ADC12ON: " + adc12On);
			if (adc12On && enableConversion && startConversion && !isConverting) {
				// Set the start time to be now!
				isConverting = true;
				adc12Pos = startMem;
				int delay = clockPredivider * adcDiv
						* ((adc12Pos < 8 ? shTime0 : shTime1) + 13);
				cpu.scheduleTimeEvent(adcTrigger, cpu.getTime() + delay);
			}
			break;
		case ADC12CTL1:
			if (enableConversion) {
				// Ongoing conversion: only some parts may be changed
				adc12ctl1 = (adc12ctl1 & 0xfff8) + (value & 0x6);
			} else {
				adc12ctl1 = value & 0xfffe;
				startMem = (value >> 12) & 0xf;
				shSource = (value >> 10) & 0x3;
				adcDiv = ((value >> 5) & 0x7) + 1;
				adcSSel = (value >> 3) & 0x03;
			}
			conSeq = (value >> 1) & 0x03;
			if (DEBUG)
				log("Set startMem: " + startMem + " SHSource: " + shSource
						+ " ConSeq-mode:" + conSeq + " Div: " + adcDiv + " ADCSSEL: "
						+ adcSSel);
			break;

		case ADC12CTL2: /* Low Power Specs */
			if (enableConversion) {
				/*
				 * Clock pre divider can't be modified when conversion is already
				 * enabled
				 */
				value &= 0xfeff;
				value |= (adc12ctl2 & 0x100);
			}
			clockPredivider = ((value & 0x100) > 0) ? 4 : 1;
			/* bit resolution 8, 10, 12 */
			int tmp = (value & 0x30) >> 4;
			tmp = (tmp <= 2) ? tmp * 2 : 4;
			bitsResolution = tmp + 8;
			formatSigned = ((value & 0x08) > 0);
			if (formatSigned) {
				logw(WarningType.EMULATION_ERROR, "signed format not implemented");
			}
			adc12ctl2 = value;
			break;
		case ADC12IFGR0:
			adc12ifgr0 = value;
			break;
		case ADC12IFGR1:
			adc12ifgr1 = value;
			break;
		case ADC12IFGR2:
			adc12ifgr2 = value;
			break;
		case ADC12IER0:
			adc12ier0 = value;
			break;
		case ADC12IER1:
			adc12ier1 = value;
			break;
		case ADC12IER2:
			adc12ier2 = value;
			break;
		default:
			if (address >= ADC12MCTL0 && address <= ADC12MCTL31) {
				if (enableConversion) {
					/* Ongoing conversion: not possible to modify */
				} else {
					address = ((address - ADC12MCTL0) / 2); 
					adc12mctl[address] = value & 0xff;
					if (DEBUG)
						log("ADC12MCTL" + address + " source = "
								+ (value & 0xf)
								+ (((value & EOS_MASK) != 0) ? " EOS bit set" : ""));
				}
			}
		}
	}

	// read a value from the IO unit
	public int read(int address, boolean word, long cycles) {
		address -= offset;
		switch (address) {
		case ADC12CTL0:
			return adc12ctl0;
		case ADC12CTL1:
			return isConverting ? (adc12ctl1 | BUSY_MASK) : adc12ctl1;
		case ADC12IFGR0:
			return adc12ifgr0;
		case ADC12IFGR1:
			return adc12ifgr1;
		case ADC12IFGR2:
			return adc12ifgr2;
		case ADC12IER0:
			return adc12ier0;
		case ADC12IER1:
			return adc12ier1;
		case ADC12IER2:
			return adc12ier2;
		default:
			if (address >= ADC12MCTL0 && address <= ADC12MCTL31) {
				return adc12mctl[address - ADC12MCTL0];
			} else if (address >= ADC12MEM0 && address <= ADC12MEM15) {
				int reg = (address - ADC12MEM0) / 2;
				// Clear ifg!
				adc12ifgr0 &= ~(1 << reg);
				// System.out.println("Read ADCMEM" + (reg / 2));
				if (adc12iv == reg * 2 + 12) {
					cpu.flagInterrupt(adc12Vector, this, false);
					adc12iv = 0;
					// System.out.println("** de-Trigger ADC12 IRQ for ADCMEM" +
					// adc12Pos);
				}
				return adc12mem[reg];
			} else if (address >= ADC12MEM16 && address <= ADC12MEM31) {
				int reg = (address - ADC12MEM16) / 2;
				// Clear ifg!
				adc12ifgr1 &= ~(1 << reg);
				// System.out.println("Read ADCMEM" + (reg / 2));
				if (adc12iv == reg * 2 + 12) {
					cpu.flagInterrupt(adc12Vector, this, false);
					adc12iv = 0;
					// System.out.println("** de-Trigger ADC12 IRQ for ADCMEM" +
					// adc12Pos);
				}
				return adc12mem[reg];
			}
		}
		return 0;
	}

	int smp = 0;

	private void convert() {
		// If off then just return...
		if (!adc12On) {
			isConverting = false;
			return;
		}
		boolean runAgain = enableConversion && conSeq != CONSEQ_SINGLE;
		// Some noise...
		ADCInput input = adcInput[adc12mctl[adc12Pos] & 0xf];
		int reading = input != null ? input.nextData(adc12Pos) : 2048 + 100 - smp & 255;
		/* Adapt resolution to 8, 10, 12 bits */
		/*float percent = (reading - getMinInVoltage()) * 1f
				/ ((getMaxInVoltage() - getMinInVoltage()) * 1f) - 1;
		reading = (int) (((1L << bitsResolution) - 1) * percent);*/
		adc12mem[adc12Pos] = reading;
		smp += 7;
		if(adc12Pos > 15) {
			adc12ifgr1 |= (1 << adc12Pos);
			if ((adc12ier1 & (1 << adc12Pos)) > 0) {
				// This should check if there already is an higher iv!
				adc12iv = adc12Pos * 2 + 6;
				// System.out.println("** Trigger ADC12 IRQ for ADCMEM" + adc12Pos);
				cpu.flagInterrupt(adc12Vector, this, true);
			}
		} else {
			adc12ifgr0 |= (1 << adc12Pos);
			if ((adc12ier0 & (1 << adc12Pos)) > 0) {
				// This should check if there already is an higher iv!
				adc12iv = adc12Pos * 2 + 6;
				// System.out.println("** Trigger ADC12 IRQ for ADCMEM" + adc12Pos);
				cpu.flagInterrupt(adc12Vector, this, true);
			}
		}
		if ((conSeq & CONSEQ_SEQUENCE_MASK) != 0) {
			// Increase
			if ((adc12mctl[adc12Pos] & EOS_MASK) == EOS_MASK) {
				adc12Pos = startMem;
				if (conSeq == CONSEQ_SEQUENCE) {
					// Single sequence only
					runAgain = false;
				}
			} else {
				adc12Pos = (adc12Pos + 1) & 0x0f;
			}
		}
		if (!runAgain) {
			isConverting = false;
		} else {
			int delay = clockPredivider * adcDiv
					* ((adc12Pos < 8 ? shTime0 : shTime1) + 13);
			cpu.scheduleTimeEvent(adcTrigger, adcTrigger.getTime() + delay);
		}
	}

	public void interruptServiced(int vector) {
	}

	/**
	 * Get the reference voltage if it is 1.5 volts or 2.5V
	 * 
	 * @return
	 */
	public boolean isRef25V() {
		return ref25V;
	}

}
