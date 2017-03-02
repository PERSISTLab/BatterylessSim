
#include <msp430fr6989.h>
#include "cc1101.h"
#include <string.h>
#include "packet.h"
#include <stdint.h>
#include "timekeeper.h"
#include "mayfly_program.h"
#include "printf.h"

#define SEND_RATE_HIGH 10
#define SEND_RATE_LOW 1
#define FRAM_START 0xD000

void FRAMWrite(uint16_t data);
void setupRadio ();
void storeTempToBuff (int temp);
void main_init ();
void setup_temperature_adc();
uint16_t read_temperature_adc();
uint16_t voltage_high();
uint16_t average_temperature ();
uint16_t fast_tardis(uint16_t _adc_14_value);

// Globals
int send_rate = SEND_RATE_LOW;
int count = 0;
unsigned long *FRAM_write_ptr;
uint8_t tx_buffer[61]={0};
uint8_t rx_buffer[61]={0};
int tempbuffer[SEND_RATE_HIGH];
int DEVICEID = 1; 
sync_t pkt;


int main(void)
{
  WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT

  // Maybe this will unlock the fram controller?
  FRCTL0 = FRCTLPW >> 8;
  GCCTL0 |= FRPWR;

  // Configure GPIO
  P1DIR |= BIT0;
  P1OUT |= BIT0;

  // Disable the GPIO power-on default high-impedance mode to activate
  // previously frctlpwconfigured port settings
  PM5CTL0 &= ~LOCKLPM5;

  main_init();

  // Create a packet to send, fill it out, then encode it in the buffer
  pkt = create_sync_packet(DEVICEID, 0, 1000, 0);

  FRAM_write_ptr = (unsigned long *) FRAM_START;

  /*TB0CCTL0 = CCIE;                          // TBCCR0 interrupt enabled
  TB0CCR0 = 50000;
  TB0CTL = TBSSEL__SMCLK | MC__UP;          // SMCLK, UP mode

  __bis_SR_register(LPM3_bits + GIE);       // Enter LPM3 w/ interrupt
  __no_operation();                         // For debugger
  
  while(1);*/
  while (1) {
    tempbuffer[count] = read_temperature_adc();
    //siren_command("%s", "GRAPH-EVENT: Read temperature!\n");
    FRAMWrite(tempbuffer[count]);                            // Endless loop
    siren_command("%s %i! \n", "PRINTF: Temperature = ", tempbuffer[count]);
    count++;

    if (count >= send_rate)
    {
      count = 0;
      pkt.temperature = average_temperature();
      memcpy(tx_buffer, &pkt, sizeof(pkt));
      SendData(tx_buffer,sizeof(pkt));
      //siren_command("%s", "GRAPH-EVENT: Radio send!\n");
      if (voltage_high()) {
        send_rate = SEND_RATE_HIGH;
        siren_command("%s! \n", "PRINTF: Voltage is above 3.2");
      } else {
        send_rate = SEND_RATE_LOW;
        siren_command("%s! \n", "PRINTF: Voltage is not above 3.2");
      }
      siren_command("%s%i! \n", "PRINTF: send_rate = ", send_rate);
      Idle();   
    } 
    
    pkt.local_time += 10;
    //siren_command("%s %i!\n", "PRINTF: Updated local time to:", pkt.local_time);
    delay(2000000);
  }

  return 1;
}

// Timer0_A0 interrupt service routine
#if defined(__TI_COMPILER_VERSION__) || defined(__IAR_SYSTEMS_ICC__)
#pragma vector = TIMER0_A0_VECTOR
__interrupt void Timer0_A0_ISR (void)
#elif defined(__GNUC__)
void __attribute__ ((interrupt(TIMER0_A0_VECTOR))) Timer0_A0_ISR (void)
#else
#error Compiler not supported!
#endif
{
  tempbuffer[count] = read_temperature_adc();
    siren_command("%s", "GRAPH-EVENT: Read temperature!\n");
    FRAMWrite(tempbuffer[count]);                            // Endless loop
    count++;

    if (count >= send_rate)
    {
      count = 0;
      pkt.temperature = average_temperature();
      memcpy(tx_buffer, &pkt, sizeof(pkt));
      SendData(tx_buffer,sizeof(pkt));
      siren_command("%s", "GRAPH-EVENT: Radio send!\n");
      if (voltage_high()) {
        send_rate = SEND_RATE_HIGH;
        siren_command("%s! \n", "PRINTF: Voltage is above 3.2");
      } else {
        send_rate = SEND_RATE_LOW;
        siren_command("%s! \n", "PRINTF: Voltage is not above 3.2");
      }
      siren_command("%s%i! \n", "PRINTF: send_rate = ", send_rate);
      Idle();   
    } 
    
    pkt.local_time += 10;
    siren_command("%s %i!\n", "PRINTF: Updated local time to:", pkt.local_time);
    delay(2000000);
}

void FRAMWrite(uint16_t data)
{
    *FRAM_write_ptr++ = data;
}

void setupRadio () 
{
  Init(); 
  SetDataRate(5); // Needs to be the same in Tx and Rx
  SetLogicalChannel(1);
  SetTxPower(3);
}

void storeTempToBuff (int temp) 
{
  tempbuffer[count] = temp;
}

void main_init () 
{
  setupRadio();

  setup_temperature_adc();

  /* Set TEMP char pin high */
  P4DIR |= BIT4;
  P4OUT |= BIT4;
}

void setup_temperature_adc() 
{
  ADC12CTL0 = ADC12ON | ADC12SHT0_2;        // Turn on ADC12, set sampling time
  ADC12CTL1 = ADC12SHP;                     // Use sampling timer
  ADC12CTL0 |= ADC12ENC;                    // Enable conversions
}

uint16_t read_temperature_adc() 
{
  ADC12CTL0 &= ~ADC12ENC;                    // Disable conversions to change input
  ADC12MCTL0 &= ~BIT2;                      // Input 0
  ADC12CTL0 |= ADC12ENC;                   // Re-enable conversions
  ADC12CTL0 |= ADC12SC;                   // Start conversion-software trigger
  while (!(ADC12IFGR0 & BIT0));
  return ADC12MEM0;                     // Read conversion result
}

uint16_t voltage_high() 
{

  ADC12CTL0 &= ~ADC12ENC;                    // Disable conversions to change input
  ADC12MCTL0 |= BIT2;                       // Input 4
  ADC12CTL0 |= ADC12ENC;                   // Re-enable conversions
  ADC12CTL0 |= ADC12SC;                   // Start conversion-software trigger
  while (!(ADC12IFGR0 & BIT0));
  return ADC12MEM0;                     // Read conversion result
}

uint16_t average_temperature () 
{
  int i = 0, sum = 0;
  for (; i < send_rate; i++) {
    sum += tempbuffer[i];
    tempbuffer[i] = 0; // reset temp buffer
  }
  siren_command("%s %i!\n", "PRINTF: average_temperature:", sum / send_rate);
  return sum / send_rate;
}
