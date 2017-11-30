package se.sics.mspsim.config;

import java.util.ArrayList;

import se.sics.mspsim.core.ADC12_B;
import se.sics.mspsim.core.ClockSystem;
import se.sics.mspsim.core.FrClockSystem;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.MPU;
import se.sics.mspsim.core.MSP430Config;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.Multiplier32;
import se.sics.mspsim.core.PMM;
import se.sics.mspsim.core.SysReg;
import se.sics.mspsim.core.Timer;
import se.sics.mspsim.core.eusci.EUSCI;
import se.sics.mspsim.core.eusci.EUSCI_A;
import se.sics.mspsim.core.eusci.EUSCI_B;
import se.sics.mspsim.util.Utils;

public class MSP430fr6989Config extends MSP430Config {

	// It appears from the technical sheet that the ports are the same as the MSP430f5437
	// Except that P3 and P4 have interrupt flags and there are two selections instead of one for each port and there is no drive 
	// strength on the 6989 (DS)
	// I would really like to have them be SEL0 and SEL1 but I'm trying to modify their original code as little as possible...
	private static final String portConfig[] = {
        "P1=200,IN 00,OUT 02,DIR 04,REN 06,SEL 0A,SEL2 0C,IV_L 0E,IV_H 0F,IES 18,IE 1A,IFG 1C",
        "P2=200,IN 01,OUT 03,DIR 05,REN 07,SEL 0B,SEL2 0D,IV_L 1E,IV_H 1F,IES 19,IE 1B,IFG 1D",
        "P3=220,IN 00,OUT 02,DIR 04,REN 06,SEL 0A,SEL2 0C,IV_L 0E,IV_H 0F,IES 18,IE 1A,IFG 1C", // Not sure about IV_H value
        "P4=220,IN 01,OUT 03,DIR 05,REN 07,SEL 0B,SEL2 0D,IV_L 1E,IV_H 1F,IES 19,IE 1B,IFG 1D",
        "P5=240,IN 00,OUT 02,DIR 04,REN 06,SEL 0A,SEL2 0C",
        "P6=240,IN 01,OUT 03,DIR 05,REN 07,SEL 0B,SEL2 0D",
        "P7=260,IN 00,OUT 02,DIR 04,REN 06,SEL 0A,SEL2 0C",
        "P8=260,IN 01,OUT 03,DIR 05,REN 07,SEL 0B,SEL2 0D",
        "P9=280,IN 00,OUT 02,DIR 04,REN 06,SEL 0A,SEL2 0C",
        "P:=280,IN 01,OUT 03,DIR 05,REN 07,SEL 0B,SEL2 0D", // This should be 10 I think... poor parsing
        };
	
	public MSP430fr6989Config()
	{
		/* Only 56 interrupt vectors (0 index) */
        maxInterruptVector = 55;
        MSP430XArch = true;
        // There is no flash control but this is the fram offset below which should be treated the same
        flashControllerOffset = 0x140;
        /* Special functions */
        sfrOffset = 0x100;
        
        // Use FRAM for MSP430fr6989
        useFram();
        
        /* configuration for the timers - need to set-up new source maps!!! */
        // Not sure about these first three values, or the static timer variable
        TimerConfig timerA0 = new TimerConfig(44, 43, 5, 0x340, Timer.TIMER_Ax149, "TimerA0", 0x340 + 0x2e);
        TimerConfig timerA1 = new TimerConfig(39, 38, 3, 0x380, Timer.TIMER_Ax149, "TimerA1", 0x380 + 0x2e);
        TimerConfig timerA2= new TimerConfig(36, 35, 2, 0x400, Timer.TIMER_Ax149, "TimerA2", 0x400 + 0x2e);
        TimerConfig timerA3 = new TimerConfig(33, 32, 5, 0x440, Timer.TIMER_Ax149, "TimerA3", 0x440 + 0x2e);
        TimerConfig timerB0 = new TimerConfig(51, 50, 7, 0x3C0, Timer.TIMER_Bx149, "TimerB0",  0x3C0 + 0x2e);
        timerConfig = new TimerConfig[] {timerA0, timerA1, timerA2, timerA3, timerB0};
        
        uartConfig = new UARTConfig[] {
                new UARTConfig("USCI A0", 47, 0x5c0), 
                new UARTConfig("USCI A1", 42, 0x5e0),
                // The USCI B may not be UART...
                new UARTConfig("USCI B0", 46, 0x640), // The offset range of this is 02f rather then 01f
                new UARTConfig("USCI B1", 41, 0x680) // The offset range of this is 02f rather then 01f
        };
        
        /* configure memory */
        infoMemConfig(0x1800, 128 * 4); // Same as before
        mainFlashConfig(0x4400, 127 * 1024); // This is FRAM now
        ramConfig(0x1c00, 2 * 1024); // Only 2KB of SRAM now
        ioMemSize(0x1000); /* 4 KB of Peripheral memory, page 129 of MSP430fr6989 datasheet*/
        
        watchdogOffset = 0x15c;
        // bsl, IO, etc at a later stage...
	}
	
	@Override
	public int setup(MSP430Core cpu, ArrayList<IOUnit> ioUnits) 
	{
		Multiplier32 mp = new Multiplier32(cpu, cpu.memory, 0x4c0);
        cpu.setIORange(0x4c0, 0x2f, mp); // Not sure about 0x2e... looks like 0x2f according to datasheet for both microcontrollers
        
        for (int i = 0; i < uartConfig.length; i++) {
            EUSCI usci;
            if (uartConfig[i].name.contains("A")) {
            	usci = new EUSCI_A(cpu, i, cpu.memory, this);
            } else {
            	usci = new EUSCI_B(cpu, i, cpu.memory, this);
            }
            /* setup 0 - 1f as IO addresses */
            if (uartConfig[i].name.contains("A")) {
            	cpu.setIORange(uartConfig[i].offset, 0x20, usci); // Not sure about 0x20
            } else { // Must be B
            	cpu.setIORange(uartConfig[i].offset, 0x40, usci); // Not sure about 0x20, looks like 0x40 from datasheet
            }
            ioUnits.add(usci);
        }
        
        IOPort last = null;
        ioUnits.add(last = IOPort.parseIOPort(cpu, 37, portConfig[0], last));
        ioUnits.add(last = IOPort.parseIOPort(cpu, 34, portConfig[1], last));
        ioUnits.add(last = IOPort.parseIOPort(cpu, 31, portConfig[2], last));
        ioUnits.add(last = IOPort.parseIOPort(cpu, 30, portConfig[3], last));
        
        for (int i = 4; i < portConfig.length; i++) {
            ioUnits.add(last = IOPort.parseIOPort(cpu, 0, portConfig[i], last));
        }
        
        /* XXX: Stub IO units: Sysreg and PMM */
		SysReg sysreg = new SysReg(cpu, cpu.memory);
		cpu.setIORange(SysReg.ADDRESS, SysReg.SIZE, sysreg);
		ioUnits.add(sysreg);
		
		// Same pmm, except offset range address is different
		PMM pmm = new PMM(cpu, cpu.memory, 0x120);
		cpu.setIORange(0x120, PMM.SIZE, pmm);
		ioUnits.add(pmm);
		
		// MPU
		MPU mpu = new MPU("mpu", cpu, cpu.memory, 0x05A0);
		ioUnits.add(mpu);
		cpu.setIORange(0x05A0, 16, mpu);
        
		// ADC12_B
		ADC12_B adc12 = new ADC12_B(cpu);
        ioUnits.add(adc12);
        cpu.setIORange(0x0800, 0x9f, adc12);
		
		return portConfig.length + uartConfig.length;
	}
	
	@Override
    public String getAddressAsString(int addr) {
        return Utils.hex20(addr);
    }

    @Override
    public ClockSystem createClockSystem(MSP430Core cpu, int[] memory, Timer[] timers) {
        return new FrClockSystem(cpu, memory, 0, timers);
    }

}
