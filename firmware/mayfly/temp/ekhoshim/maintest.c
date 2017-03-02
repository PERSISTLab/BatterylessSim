#include <msp430fr6989.h>
#include <stdint.h>
#include <string.h>
#include "printf.h"
#include "ekhoshim.h"
#include "timekeeper.h"
#include "mayfly_program.h"

unsigned int fast_tardis(unsigned int _adc_14_value);
void setup_temperature_adc()
{
  send_id(0);
  ADC12CTL0 = ADC12ON | ADC12SHT0_2;
  ADC12CTL1 = ADC12SHP;
  ADC12CTL0 |= ADC12ENC;
}

int read_temperature_adc()
{
  send_id(1);
  ADC12CTL0 |= ADC12SC;
  while (!(ADC12IFGR0 & BIT0))
    ;

  return ADC12MEM0;
}

int current_time;
int last_timestamp;
int current_loop_index = 0;
int main (void)
{
	main_start(21);
  send_id(3);
  WDTCTL = WDTPW | WDTHOLD;
  setup_temperature_adc();
  int adc_sample = read_temperature_adc();
  siren_command("PRINTF: ADC Temperature Read      %u\r\n", adc_sample);
  int tardisval = fast_tardis(4095);
  siren_command("PRINTF: Time Elapsed (ms) %u\r\n", tardisval);
  P1DIR |= BIT5;
  P1OUT |= BIT5;
  siren_command("PRINTF: TARDIS ON\n");
  P2DIR |= BIT4;
  P2OUT |= BIT4;
  siren_command("PRINTF: TEMP ON\n");
  current_time += tardisval / 10;
  mayfly_init(current_time);
  while (1)
  {
    send_id(4);
    adc_sample = read_temperature_adc();
    siren_command("PRINTF: ADC Temperature Read      %i\r\n", adc_sample);
  }

}
