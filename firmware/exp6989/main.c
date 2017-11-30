/*
	MPU tests, four segments, MPU can protect all addressable memory (registers, SRAM, FRAM)

	Run this firmware on modified SIREN with the following arguments:

	-nogui -exitwhendone -platform=exp6989 firmware/exp6989/main.out -autorun="scripts/simple.sc"

*/

#include <stdint.h>
#include <string.h>
#include "printf.h"
#include <msp430fr6989.h>

// Macros for supporting adding an extra segment
sfr_w(MPUSEGB3);                              /* MPU Segmentation Border 3 Register */
sfr_b(MPUSEGB3_L);                            /* MPU Segmentation Border 3 Register */
sfr_b(MPUSEGB3_H);                            /* MPU Segmentation Border 3 Register */

#define MPUSEG4IFG            (0x0020)       /* MPU Main Memory Segment 4 violation interupt flag */
#define MPUSEG4RE             (0x0100)       /* MPU Main memory Segment 4 Read enable */
#define MPUSEG4WE             (0x0200)       /* MPU Main memory Segment 4 Write enable */
#define MPUSEG4XE             (0x0400)       /* MPU Main memory Segment 4 Execute enable */

int main (void)
{
	WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT
	uint8_t i=0;
	uint16_t readval = 0;
	siren_command("PRINTF: Start\r\n");

    // Reads
	MPUCTL0  = MPUPW;
    MPUSEGB1 = 0x021000 >> 4;
    MPUSEGB2 = 0x022000 >> 4;
    MPUSEGB3 = 0x023000 >> 4;
    MPUSAM  &= ~MPUSEG2RE; // seg 2 no read
    MPUSAM  &= ~MPUSEG3RE; // seg 3 no read
    MPUSAM  &= ~MPUSEG4RE; // seg 4 no read

    MPUCTL0  = MPUPW | MPUENA | MPUSEGIE;

    unsigned long *ptr = (unsigned long *)0x20FE0;

	while(ptr < (unsigned long *)0x23F40) {
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
		if(mpuctl1val & MPUSEG4IFG) {
			siren_command("PRINTF: Violation (read) in segment 4, addr: 0x%x\r\n", ptr);    
		}		
		siren_command("PRINTF: addr: 0x%x value: 0x%x\r\n", ptr, readval);
		readval = 0;


		ptr+=32;
	}

	// Writes
	MPUCTL0  = MPUPW;
    MPUSEGB1 = 0x021000 >> 4;
    MPUSEGB2 = 0x022000 >> 4;
    MPUSEGB3 = 0x023000 >> 4;
    MPUSAM  &= ~MPUSEG2WE; // seg 2 no writes
    MPUSAM  &= ~MPUSEG3WE; // seg 3 no writes
    MPUSAM  &= ~MPUSEG4WE; // seg 4 no writes

    MPUCTL0  = MPUPW | MPUENA | MPUSEGIE;

    ptr = (unsigned long *)0x20FE0;
    while(ptr < (unsigned long *)0x23F40) {
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
		if(mpuctl1val & MPUSEG4IFG) {
			siren_command("PRINTF: Violation (write) in segment 4, addr: 0x%x\r\n", ptr);    
		}				
		siren_command("PRINTF: addr: 0x%x value: 0x%x\r\n", ptr, readval);
		readval = 0;

		ptr+=32;
	}

	// Execute protect
	MPUCTL0  = MPUPW;
    MPUSEGB1 = 0x021000 >> 4;
    MPUSEGB2 = 0x022000 >> 4;
    MPUSEGB3 = 0x023000 >> 4;
    MPUSAM  &= ~MPUSEG4XE; // seg 4 no execution

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
	if(mpuctl1val & MPUSEG4IFG) {
		siren_command("PRINTF: Violation (exec) in segment 4, addr: 0x%x\r\n", ptr);    
	}	
	
	// Done with test
	siren_command("PRINTF: Done.\r\n");
	while(1);
	

}