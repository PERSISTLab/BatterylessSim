Known issues:
 * MAX_MEM in MSP430Core.java is too large (64KB vs. 200B), but setting it to
   the correct size for MSP430F2132 causes ArrayIndexOutOfBoundsException.

For an example of checkpt command look in scripts/wisp.sc
