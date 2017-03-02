#include <msp430fr6989.h>
#include <stdint.h>
#include <string.h>
#include "printf.h"
#include "timekeeper.h"
#include "mayfly_program.h"

uint16_t fast_tardis(uint16_t _adc_14_value);

void setup_temperature_adc() {
  ADC12CTL0 = ADC12ON | ADC12SHT0_2;        // Turn on ADC12, set sampling time
  ADC12CTL1 = ADC12SHP;                     // Use sampling timer
  ADC12CTL0 |= ADC12ENC;                    // Enable conversions
}

uint16_t read_temperature_adc() {
  ADC12CTL0 |= ADC12SC;                   // Start conversion-software trigger
  while (!(ADC12IFGR0 & BIT0));
  return ADC12MEM0;                     // Read conversion result
}

/*void setup_temperature_adc() {
  ADC12CTL0 &= ~ADC12ENC;                         // Reset ADC12
  ADC12MCTL3 = ADC12INCH_4;                       // Read on ADC channel starting at 4
  ADC12CTL0 = ADC12ON | ADC12SHT0_2;
  ADC12IER0 = 0x10;                           // Enable ADC12IFG.4 
  ADC12CTL1 = ADC12SHP;                     // Use sampling timer
  //ADC12MCTL4 = ADC12INCH_5;
  //ADC12MCTL5 = ADC12INCH_6;
  ADC12CTL0 |= ADC12ENC;                    // Enable conversion
  mspsim_printf("Setup temperature adc\n");
}

uint16_t read_temperature_adc() {
  ADC12CTL0 |= ADC12SC;                   // Start conversion-software trigger
  mspsim_printf("before loop\n");
  while(!(ADC12IFGR0 & BIT4));
  mspsim_printf("after loop\n");
  return ADC12MEM4;                     // Read conversion result
}*/

int last_timestamp;
uint8_t current_loop_index __attribute__ ((section (".upper.rodata" ))); // In priority list
int main(void) {
  WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT

  setup_temperature_adc();
  uint16_t adc_sample = read_temperature_adc();

  // Temperature reading
  siren_command("PRINTF: ADC Temperature Read      %u\r\n", adc_sample);
  uint16_t tardisval = fast_tardis(4095);

  // Temperature sensor reading
  siren_command("PRINTF: Time Elapsed (ms) %u\r\n", tardisval);

  // Set TARDIS charge pin high */
  P1DIR |= BIT5;
  P1OUT |= BIT5;
  siren_command("PRINTF: TARDIS ON\n");

  /* Set TEMP char pin high */
  P2DIR |= BIT4;
  P2OUT |= BIT4;
  siren_command("PRINTF: TEMP ON\n"); 

  mayfly_init();

  while(1) {
    mayfly_main_loop();
    //mspsim_printf("Loop %u\n", current_loop_index++);
    adc_sample = read_temperature_adc();
    siren_command("PRINTF: ADC Temperature Read      %i\r\n", adc_sample);
  }
}
