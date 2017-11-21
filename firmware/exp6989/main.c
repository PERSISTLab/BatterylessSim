
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
	siren_command("PRINTF: Loop index%u\r\n", i++);    
	__delay_cycles(1000);
	}
}