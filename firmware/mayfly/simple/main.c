#include <msp430fr6989.h>
#include <stdint.h>
#include <string.h>

#include "printf.h"

#include "timekeeper.h"
#include "mayfly_program.h"

uint8_t current_loop_index __attribute__ ((section (".upper.rodata" ))); // In priority list
uint16_t fast_tardis(uint16_t _adc_14_value);

void setup_timekeeper_adc() {
  ADC12CTL0 = ADC12ON | ADC12SHT0_2;        // Turn on ADC12, set sampling time
  ADC12CTL1 = ADC12SHP;                     // Use sampling timer
  ADC12IER0 = 0x1;                           // Enable ADC12IFG.0
  ADC12CTL0 |= ADC12ENC;                    // Enable conversions
}

uint16_t read_timekeeper_adc() {
  ADC12CTL0 |= ADC12SC;                   // Start conversion-software trigger
  while (!(ADC12IFGR0 & BIT0));
  return ADC12MEM0;                     // Read conversion result
}

uint32_t current_time;  // Load from FRAM, this is the time since the duty cycle started in ms
            // This is the number of deciseconds since the begining of time
 
int main(void) {
  WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT
  mspsim_printf("Right at the begining\n");
  setup_timekeeper_adc();
  uint16_t adc_sample = read_timekeeper_adc();
  // RemTim reading
  uint16_t tardisval = fast_tardis(adc_sample);
  mspsim_printf("ADC Read      %u\r\n", adc_sample);
  mspsim_printf("Time Elapsed (ms) %u\r\n", tardisval);

  // Set TARDIS charge pin high */
  P1DIR |= BIT5;
  P1OUT |= BIT5;
  mspsim_printf("TARDIS ON\n");

  current_time+=tardisval / 10;

  mayfly_init(current_time);

  while(1) {    
    volatile j = 0;
    mspsim_printf("here\n");
    mayfly_schedule(current_time);
    mspsim_printf("here2\n");
    mspsim_printf("Loop %u\n", current_loop_index++);
    //for (j = 0; j < 1000; j++);
  }

  return 0;
}
