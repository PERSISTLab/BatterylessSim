
#include "printf.h"
#include <msp430.h>

int main (void)
{
	/*WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT

  	// Configure GPIO
  	P1OUT &= ~BIT0;                           // Clear P1.0 output latch for a defined power-on state
  	P1DIR |= BIT0;                            // Set P1.0 to output direction
	*/
  	while(1)
		printf("work\n");
}