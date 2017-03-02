#include <msp430fr6989.h>
#include <stdint.h>
#include <string.h>
#include "printf.h"
#include "timekeeper.h"
#include "mayfly_program.h"

uint16_t fast_tardis(uint16_t _adc_14_value);

void setup_timekeeper_adc() {
  ADC12CTL0 = ADC12ON | ADC12SHT0_2;        // Turn on ADC12, set sampling time
  ADC12CTL1 = ADC12SHP;                     // Use sampling timer
  ADC12MCTL0 = ADC12VRSEL_4;                // Vr+ = VeREF+ (ext) and Vr-=AVss
  ADC12CTL0 |= ADC12ENC;                    // Enable conversions
}

uint16_t read_timekeeper_adc() {
  ADC12CTL0 |= ADC12SC;                   // Start conversion-software trigger
  while (!(ADC12IFGR0 & BIT0));
  return ADC12MEM0;                     // Read conversion result
}

int last_timestamp;
int main(void) {
  WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT
  
  setup_timekeeper_adc();
  uint16_t adc_sample = read_timekeeper_adc();
  uint16_t tardisval = fast_tardis(adc_sample);
  siren_command("PRINTF: ADC Read      %u\r\n", adc_sample);
  siren_command("PRINTF: Time Elapsed (ms) %u\r\n", tardisval);

  // Set TARDIS charge pin high */
  P1DIR |= BIT5;
  P1OUT |= BIT5;
  siren_command("PRINTF: TARDIS ON\n");

  // start timer / RT
  int sum = 0;
  int i=0;
  while(1) {
    mayfly_main_loop();
    siren_command("GRAPH-EVENT: Loop\n");
  }
}
