#include <msp430f2618.h>
#include <stdint.h>
#include <string.h>
#include "printf.h"

int last_timestamp;
int main(void) {
  WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT
  DCOCTL = 0;                               // Select lowest DCOx and MODx settings
  BCSCTL1 = CALBC1_1MHZ;                    // Set DCO
  DCOCTL = CALDCO_1MHZ; 
  
  // Init USCI_A0 on p3.4 and p3.5
  P3SEL = 0x30;                             // P3.4,5 = USCI_A0 TXD/RXD
  UCA0CTL1 |= UCSSEL_2;                     // SMCLK
  UCA0BR0 = 8;                              // 1MHz 115200
  UCA0BR1 = 0;                              // 1MHz 115200
  UCA0MCTL = UCBRS2 + UCBRS0;               // Modulation UCBRSx = 5
  UCA0CTL1 &= ~UCSWRST;                     // **Initialize USCI state machine**

  // ADC read for RemTim
  P6SEL |= BIT7;                            // Enable A/D channel A7
  ADC12CTL0 = ADC12ON + SHT0_15 + SHT1_15; // Turn on and set up ADC12 ,no reference since has 1.8V regulator
  ADC12CTL1 = ADC12DIV_0;                          // Use sampling timer, no clock division
  ADC12MCTL0 = INCH_0;                      // Ref = +=AVCC and -=AVSS, input is A0
  for (volatile uint8_t i=0; i<60; i++);                // Delay for reference start-up (30us according to datasheet)
  ADC12CTL0 |= ENC;                         // Enable conversions
  
  ADC12CTL0 |= ADC12SC;                   // Start convn
  while ((ADC12IFG & BIT0)==0);
  uint16_t adc_sample = ADC12MEM0;
  siren_command("PRINTF: ADC Read      %u\r\n", adc_sample);

  // start timer / RT
  int sum = 0;
  int i=0;
  while(1) {
    ADC12CTL0 |= ADC12SC;                   // Start convn
    while ((ADC12IFG & BIT0)==0);
    adc_sample = ADC12MEM0;
    siren_command("PRINTF: ADC Read      %u\r\n", adc_sample);    
    uint32_t* __w_ptr_argh = (uint32_t*)0x28ff18;
    *__w_ptr_argh = i;
    __delay_cycles(1000);
  }
  //__bis_SR_register(LPM3_bits);
  //__no_operation();                         // For debugger
}
