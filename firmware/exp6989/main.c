
#include <stdint.h>
#include <string.h>
#include "printf.h"
#include <msp430fr6989.h>

int main (void)
{
	WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT
	uint8_t i=0;
	uint16_t readval = 0;
	siren_command("PRINTF: Start\r\n");

    // Reads
	MPUCTL0  = MPUPW;
    MPUSEGB1 = 0x021000 >> 4;
    MPUSEGB2 = 0x023000 >> 4;
    MPUSAM  &= ~MPUSEG2RE; // seg 2 no read

    MPUCTL0  = MPUPW | MPUENA | MPUSEGIE;

    unsigned long *ptr = (unsigned long *)0x021000;

	while(i<100) {
		siren_command("PRINTF: Loop index %u\r\n", i++);
		readval = *ptr;
		// Check access violation
		uint16_t mpuctl1val = MPUCTL1;
		if(mpuctl1val & MPUSEG1IFG) {
			siren_command("PRINTF: Violation (read) in segment 1, addr: 0x%x\r\n", ptr);    
		}
		if(mpuctl1val & MPUSEG2IFG) {
			siren_command("PRINTF: Violation (read) in segment 2, addr: 0x%x\r\n", ptr);    
		}
		if(mpuctl1val & MPUSEG3IFG) {
			siren_command("PRINTF: Violation (read) in segment 3, addr: 0x%x\r\n", ptr);    
		}
		siren_command("PRINTF: addr: 0x%x value: 0x%x\r\n", ptr, readval);
		readval = 0;


		ptr++;
	}

	// Writes
	MPUCTL0  = MPUPW;
    MPUSEGB1 = 0x021000 >> 4;
    MPUSEGB2 = 0x023000 >> 4;
    MPUSAM  &= ~MPUSEG2WE; // seg 2 no writes

    MPUCTL0  = MPUPW | MPUENA | MPUSEGIE;

    ptr = (unsigned long *)0x021000;
    i = 0;
    while(i<100) {
		siren_command("PRINTF: Loop index %u\r\n", i++);
		*ptr = 1;
		// Check access violation
		uint16_t mpuctl1val = MPUCTL1;
		if(mpuctl1val & MPUSEG1IFG) {
			siren_command("PRINTF: Violation (write) in segment 1, addr: 0x%x\r\n", ptr);    
		}
		if(mpuctl1val & MPUSEG2IFG) {
			siren_command("PRINTF: Violation (write) in segment 2, addr: 0x%x\r\n", ptr);    
		}
		if(mpuctl1val & MPUSEG3IFG) {
			siren_command("PRINTF: Violation (write) in segment 3, addr: 0x%x\r\n", ptr);    
		}
		siren_command("PRINTF: addr: 0x%x value: 0x%x\r\n", ptr, readval);
		readval = 0;


		ptr++;
	}

	// Execute protect
	MPUCTL0  = MPUPW;
    MPUSEGB1 = 0x021000 >> 4;
    MPUSEGB2 = 0x023000 >> 4;
    MPUSAM  &= ~MPUSEG3XE; // seg 3 no execution

    MPUCTL0  = MPUPW | MPUENA | MPUSEGIE;

    ((int (*)(void))0x023002)();

	uint16_t mpuctl1val = MPUCTL1;
	if(mpuctl1val & MPUSEG1IFG) {
		siren_command("PRINTF: Violation (exec) in segment 1, addr: 0x%x\r\n", ptr);    
	}
	if(mpuctl1val & MPUSEG2IFG) {
		siren_command("PRINTF: Violation (exec) in segment 2, addr: 0x%x\r\n", ptr);    
	}
	if(mpuctl1val & MPUSEG3IFG) {
		siren_command("PRINTF: Violation (exec) in segment 3, addr: 0x%x\r\n", ptr);    
	}

	// Busy wait
	while(1) {
		siren_command("PRINTF: Done.\r\n");
		__delay_cycles(100000);    	
	}

}