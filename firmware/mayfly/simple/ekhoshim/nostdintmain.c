#include <msp430fr6989.h>
#include <stdint.h>
#include <string.h>

#include "../../../lib/printf.h"
#include "timekeeper.h" // Different timekeeper
#include "mayfly_program.h"
#include "../../../lib/ekhoshim.h"


int current_loop_index = 0;//__attribute__ ((section (".upper.rodata" ))); // In priority list
unsigned int fast_tardis(unsigned int _adc_14_value);

void never_called() {};
void dont_call_either() {};

void setup_timekeeper_adc() {
  ADC12CTL0 = ADC12ON | ADC12SHT0_2;        // Turn on ADC12, set sampling time
  ADC12CTL1 = ADC12SHP;                     // Use sampling timer
  ADC12IER0 = 0x1;                           // Enable ADC12IFG.0
  ADC12CTL0 |= ADC12ENC;                    // Enable conversions
}

int read_timekeeper_adc() {
  ADC12CTL0 |= ADC12SC;                   // Start conversion-software trigger
  while (!(ADC12IFGR0 & BIT0));
  return ADC12MEM0;                     // Read conversion result
}

int current_time;  // Load from FRAM, this is the time since the duty cycle started in ms
            // This is the number of deciseconds since the begining of time
 
int main(void) {
  WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT
  setup_timekeeper_adc();
  unsigned int adc_sample = read_timekeeper_adc();
  // RemTim reading
  unsigned int tardisval = fast_tardis(adc_sample);
  siren_command("PRINTF: ADC Read      %u\r\n", adc_sample);
  siren_command("PRINTF: Time Elapsed (ms) %u\r\n", tardisval);

  // Set TARDIS charge pin high */
  P1DIR |= BIT5;
  P1OUT |= BIT5;
  siren_command("PRINTF: TARDIS ON\n");

  current_time+=tardisval / 10;

  mayfly_init(current_time);

  while(1) {    
    siren_command("PRINTF: Slightly Different\n");
    mayfly_schedule(current_time);
    siren_command("GRAPH-EVENT: Loop\n");
    //for (j = 0; j < 1000; j++);
  }

  return 0;
}
