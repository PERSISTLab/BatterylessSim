#include <msp430fr6989.h>
#include "printf.h"

volatile unsigned int ADCvar;

int main(void)
{
  WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT
  siren_command("PRINTF: Test0\n");
  // Configure GPIO
  P1SEL1 |= BIT0;                           // Enable A/D channel A0
  P1SEL0 |= BIT0;

  
  siren_command("PRINTF: Test1\n");
  // Disable the GPIO power-on default high-impedance mode to activate
  // previously configured port settings
  PM5CTL0 &= ~LOCKLPM5;
  siren_command("PRINTF: Test2\n");
  // Configure ADC12
  ADC12CTL0 = ADC12ON | ADC12SHT0_2;        // Turn on ADC12, set sampling time
  siren_command("PRINTF: Test3\n");
  ADC12CTL1 = ADC12SHP;                     // Use sampling timer
  ADC12MCTL0 = ADC12VRSEL_4;                // Vr+ = VeREF+ (ext) and Vr-=AVss
  ADC12CTL0 |= ADC12ENC;                    // Enable conversions

  while (1)
  {
    ADC12CTL0 |= ADC12SC;                   // Start conversion-software trigger
    while (!(ADC12IFGR0 & BIT0));
    ADCvar = ADC12MEM0;                     // Read conversion result
    __no_operation();                       // SET BREAKPOINT HERE
    siren_command("PRINTF: ADC read:    %u\n", ADCvar);
    __delay_cycles(100000);
  }
}