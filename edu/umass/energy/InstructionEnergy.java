package edu.umass.energy;

public class InstructionEnergy {
    /* These measurements assume a static power consumption over all voltages.
     * That is not correct, but there is no timing information available in the
     * spreadsheet containing current measurements at a range of voltages.
     */
    // "wisp cap voltage measurements" spreadsheet cell K709
    public static final double DEFAULT = 1.155e-9;

    // "wisp cap voltage measurements" spreadsheet cell K415 (/ 4)
    public static final double FLASH_WRITE_WORD = 1.345e-6;

    // "wisp cap voltage measurements" spreadsheet cell K720 XXX
    public static final double ADC_READ = 0.244e-6;
}
