package se.sics.mspsim.config;

public class MSP430f2618Config extends MSP430f2617Config {
    /* The only difference between MSP430F2617 and MSP430F2618 is that the
     * F2617 has 92KB+256B of flash and the F2618 has 116KB+256B of flash. */
    public MSP430f2618Config() {
        super();
        mainFlashConfig(0x3100, 116 * 1024);
    }
}
