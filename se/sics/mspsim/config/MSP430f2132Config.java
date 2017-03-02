package se.sics.mspsim.config;

import java.util.ArrayList;
import se.sics.mspsim.core.DMA;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.InterruptMultiplexer;
import se.sics.mspsim.core.MSP430Config;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.Multiplier;
import se.sics.mspsim.core.Timer;
import se.sics.mspsim.core.USART;

public class MSP430f2132Config extends MSP430Config {
  /* offset from beginning of interrupt table, since the beginning of the
   * interrupt table is either 0xFFC0 or 0xFFE0, depending on how you
   * interpret the MSP430F2132 datasheet. */
  int ivStart = 0xFFE0; // or 0xFFC0?
  int o = 0;            // or 16?

  // Input map for Timer1_A2 (MSP430F2132)
  public static final int[] Timer1_A2InputMap = new int[] {
    Timer.SRC_PORT + 0x10, Timer.SRC_ACLK,
      Timer.SRC_SMCLK, Timer.SRC_PORT + 0x21,     // Timer
    Timer.SRC_PORT + 0x11, Timer.SRC_PORT + 0x36,
      Timer.SRC_GND, Timer.SRC_VCC,               // Cap 0
    Timer.SRC_PORT + 0x37, Timer.SRC_CAOUT,
      Timer.SRC_GND, Timer.SRC_VCC                // Cap 1
  };

  public MSP430f2132Config () {
    maxInterruptVector = (0xFFFE - ivStart) / 2; // 15 or 31

    // Use Flash for MSP430f5437
    useFlash();
    
    TimerConfig timer0_A3 = new TimerConfig(
        9+o,   // interrupt vector for timer CCR0
        8+o,   // interrupt vector for other CCRx's
        3,     // number of CCRs
        0x160, // address of timer control register
        Timer.TIMER_Ax149, // input map ("signal connections" table)
        "Timer0_A3", // name
        Timer.TAIV // interrupt vector (from peripheral file map)
        );
    TimerConfig timer1_A2 = new TimerConfig(
        13+o,
        12+o,
        2,
        0x180,
        Timer1_A2InputMap,
        "Timer1_A2",
        Timer.TBIV
        );
    timerConfig = new TimerConfig[] {timer0_A3, timer1_A2};

    infoMemConfig(0x1000, 128 * 2);
    mainFlashConfig(0xe000, 8 * 1024);
    ramConfig(0x200, 512);
  }

  public int setup (MSP430Core cpu, ArrayList<IOUnit> ioUnits) {
    // XXX no USARTs in this chip.  can we do USCI?

    Multiplier mp = new Multiplier(cpu, cpu.memory, 0);
    cpu.setIORange(0x130, 0x0f, mp);
    ioUnits.add(mp);

    // no DMA

    // IO ports.  Ports 1 and 2 can generate interrupts
    ioUnits.add(new IOPort(cpu, 1, 2+o, cpu.memory, 0x20)); // IO1
    ioUnits.add(new IOPort(cpu, 2, 3+o, cpu.memory, 0x28)); // IO2

    // ports 3 and 4 cannot generate interrupts
    ioUnits.add(new IOPort(cpu, 3, 0, cpu.memory, 0x18));
    ioUnits.add(new IOPort(cpu, 4, 0, cpu.memory, 0x1c));
    
    // XXX ADC10

    return 3 + 6; // XXX why?
  }
}
