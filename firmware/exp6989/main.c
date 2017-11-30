
#include <stdint.h>
#include <string.h>
#include "printf.h"
#include <msp430fr6989.h>

int main (void)
{
	WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT
	uint8_t i=0;
	siren_command("PRINTF: Start\r\n");
	MPUCTL0  = MPUPW;
    MPUSEGB1 = 0x4400 >> 4;
    MPUSEGB2 = 0x023FFF >> 4;
    MPUSAM   &= ~MPUSEG2WE;
    MPUCTL0  = MPUPW | MPUENA | MPUSEGIE;

    unsigned long *ptr = (unsigned long *)0x4401;

	while(1) {
		siren_command("PRINTF: Loop index%u\r\n", i++);    
		*ptr =1;

		// Check access violation
		if(MPUCTL1 & MPUSEG2IFG) {
			siren_command("PRINTF: Bad write to segment 2, addr: %x\r\n", ptr);    
		}
		__delay_cycles(1000);
	}
}