
#include <stdint.h>
#include <string.h>
#include "printf.h"
#include <msp430fr6989.h>

int main (void)
{
	WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT
	uint8_t i=0;
	siren_command("PRINTF: Start\r\n");    
	while(1) {
	siren_command("PRINTF: ADC Read      %u\r\n", i++);    
    long* __w_ptr_argh = (long*)0x10000;
    *__w_ptr_argh = 0xDEED;	
	__delay_cycles(1000);
	}
}