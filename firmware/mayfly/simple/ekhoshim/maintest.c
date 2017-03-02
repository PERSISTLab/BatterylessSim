#include <msp430fr6989.h>
#include <stdint.h>
#include <string.h>
#include "../../../lib/printf.h"
#include "timekeeper.h" // Different timekeeper
#include "mayfly_program.h"
#include "../../../lib/ekhoshim.h"

int current_loop_index = 0;
unsigned int fast_tardis(unsigned int _adc_14_value);
void never_called()
{
  send_id(0);
}

void dont_call_either()
{
  send_id(1);
}

void setup_timekeeper_adc()
{
  send_id(2);
  ADC12CTL0 = ADC12ON | ADC12SHT0_2;
  ADC12CTL1 = ADC12SHP;
  ADC12IER0 = 0x1;
  ADC12CTL0 |= ADC12ENC;
}

int read_timekeeper_adc()
{
  send_id(3);
  ADC12CTL0 |= ADC12SC;
  while (!(ADC12IFGR0 & BIT0))
    ;

  return ADC12MEM0;
}

int current_time;
int main (void)
{
	main_start(23);
  send_id(4);
  WDTCTL = WDTPW | WDTHOLD;
  setup_timekeeper_adc();
  unsigned int adc_sample = read_timekeeper_adc();
  unsigned int tardisval = fast_tardis(adc_sample);
  mspsim_printf("ADC Read      %u\r\n", adc_sample);
  mspsim_printf("Time Elapsed (ms) %u\r\n", tardisval);
  P1DIR |= BIT5;
  P1OUT |= BIT5;
  mspsim_printf("TARDIS ON\n");
  current_time += tardisval / 10;
  mayfly_init(current_time);
  while (1)
  {
    send_id(5);
    mayfly_schedule(current_time);
    mspsim_printf("Loop %u\n", current_loop_index++);
  }

  return 0;
}
