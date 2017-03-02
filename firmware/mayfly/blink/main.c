#include <msp430fr6989.h>
#include "printf.h"

int main(void)
{
    WDTCTL = WDTPW | WDTHOLD;               // Stop WDT

    // Configure GPIO
    P1DIR |= BIT0;                          // Clear P1.0 output latch for a defined power-on state
    P1OUT |= BIT0;                          // Set P1.0 to output direction

    PM5CTL0 &= ~LOCKLPM5;                   // Disable the GPIO power-on default high-impedance mode
                                            // to activate previously configured port settings
    while(1)
    {
        siren_command("PRINTF: Blink...\n");
        P1OUT ^= BIT0;                      // Toggle LED
        __delay_cycles(1000000);
    }
}
