#include <msp430f2618.h>
#include <stdint.h>
#include <string.h>
#include "printf.h"
#include "timekeeper.h"

uint16_t fast_tardis(uint16_t _adc_14_value);
/*
SPECIFICATION:

// Tasks
add() -> (int num)
sum(int nums[]) -> ()

// Flows
add -> sum

// Dependencies
add -> sum {collect(10, 100ms)} // Get any 10 samples in a 100 millisecond interval
 */

typedef struct sum_data {
  int count[10];
  int timestamps[10];
  int length;
  int last_index;
} sum_data_t;

sum_data_t sum_data_instance;

void add_data(int datapt) {
  
}

void task_add() {
  
}

void task_sum() {
  int sum = 0;
  for(int i = 0;i < sum_data_instance.length;i++) {
    sum += sum_data_instance.count[i];
  }
  sum /= sum_data_instance.length;
} 

int io_putchar(int c) {
    while (!(IFG2&UCA0TXIFG));                // USCI_A0 TX buffer ready?
    UCA0TXBUF = c;
    return 0;
}

int io_puts_no_newline(const char *str) {
  volatile uint8_t len_str = strlen(str);
  for(volatile uint8_t i=0;i<len_str;i++) {
    while (!(IFG2&UCA0TXIFG));                // USCI_A0 TX buffer ready?
    UCA0TXBUF = str[i];
  }
  return 0;
}

void uart_send(char* str) {
  volatile uint8_t len_str = strlen(str);
  for(volatile uint8_t i=0;i<len_str;i++) {
    while (!(IFG2&UCA0TXIFG));                // USCI_A0 TX buffer ready?
    UCA0TXBUF = str[i];
  }
}

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
  ADC12MCTL0 = INCH_7;                      // Ref = +=AVCC and -=AVSS, input is A7
  for (volatile uint8_t i=0; i<60; i++);                // Delay for reference start-up (30us according to datasheet)
  ADC12CTL0 |= ENC;                         // Enable conversions
  
  ADC12CTL0 |= ADC12SC;                   // Start convn
  while ((ADC12IFG & BIT0)==0);
  uint16_t adc_sample = ADC12MEM0;
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
    siren_command("GRAPH-EVENT: Loop");
    task_sum();
    __delay_cycles(1000);
  }
  //__bis_SR_register(LPM3_bits);
  //__no_operation();                         // For debugger
}
