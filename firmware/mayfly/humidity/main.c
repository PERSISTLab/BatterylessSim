#include <msp430fr6989.h>
#include <stdint.h>
#include <string.h>
#include "printf.h"
#include "timekeeper.h"
#include "mayfly_program.h"

uint16_t fast_tardis(uint16_t _adc_14_value);

volatile unsigned char RXData;

void setup_usci_b0() {
  // Configure GPIO
  P1OUT &= ~BIT0;                           // Clear P1.0 output latch
  P1DIR |= BIT0;                            // For LED
  P1SEL0 |= BIT6 | BIT7;                    // I2C pins

  // Disable the GPIO power-on default high-impedance mode to activate
  // previously configured port settings
  PM5CTL0 &= ~LOCKLPM5;

  // Configure USCI_B0 for I2C mode
  UCB0CTLW0 |= UCSWRST;                     // Software reset enabled
  UCB0CTLW0 |= UCMODE_3 | UCMST | UCSYNC;   // I2C mode, Master mode, sync
  UCB0CTLW1 |= UCASTP_2;                    // Automatic stop generated
                                            // after UCB0TBCNT is reached
  UCB0BRW = 0x0008;                         // baudrate = SMCLK / 8
  UCB0TBCNT = 0x0002;                       // number of bytes to be received
  UCB0I2CSA = 0x0040;                       // Slave address
  UCB0CTLW1 &= ~UCSWRST;
  UCB0IE |= UCRXIE | UCNACKIE | UCBCNTIE;
}

uint16_t read_humidity_adc() {
  return 1;
}

int last_timestamp;
uint8_t current_loop_index __attribute__ ((section (".upper.rodata" ))); // In priority list
int main(void) {
  WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT

  setup_usci_b0();

  /* Set Humidity char pin high */
  P2DIR |= BIT2;
  P2OUT |= BIT2;
  mspsim_printf("HUMIDITY ON\n"); 

  mayfly_init();

  // Initial address frame
  UCB0CTLW1 |= UCTXSTT;                    // I2C start condition
  // Wait for TX buffer to be ready for new data
  while(!(UCB0IFG & UCTXIFG));
  UCB0TXBUF = 0x02; // Select CONFIG_REG

  // Wait for TX buffer to be ready for new data
  while(!(UCB0IFG & UCTXIFG));
  UCB0TXBUF = 0x00; // Set mode to 0 for only humidity measurement

  // Configure USCI_B0 for I2C mode
  UCB0CTLW0 |= UCSWRST;                     // Software reset enabled
  UCB0CTLW0 |= UCMODE_3 | UCMST | UCSYNC;   // I2C mode, Master mode, sync
  UCB0CTLW1 |= UCASTP_2;                    // Automatic stop generated
                                            // after UCB0TBCNT is reached
  UCB0BRW = 0x0008;                         // baudrate = SMCLK / 8
  UCB0I2CSA = 0x0040;                       // Slave address
  UCB0TBCNT = 0x0001;                       // change number of bytes to be received to 1 instead of 2
  UCB0CTLW1 &= ~UCSWRST;

  while (1)
  {
    __delay_cycles(20000000);

    while (UCB0CTL1 & UCTXSTP);             // Ensure stop  condition got sent
    UCB0CTLW1 |= UCTXSTT;                    // I2C start condition
    
    // Wait for TX buffer to be ready for new data
    while(!(UCB0IFG & UCTXIFG));
    UCB0TXBUF = 0x01; // Select HUMIDITY_REG

    // Wait until the last byte is completely sent
    while(UCB0STAT & UCBUSY);

    while(!(UCB0IFG & UCRXIFG));
    RXData = UCB0RXBUF;

    mspsim_printf("RXData = %i\n", RXData);

    //__bis_SR_register(LPM0_bits | GIE);     // Enter LPM0 w/ interrupt
  }
}