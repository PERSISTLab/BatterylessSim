/* --COPYRIGHT--,BSD_EX
 * Copyright (c) 2014, Texas Instruments Incorporated
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * *  Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *  Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * *  Neither the name of Texas Instruments Incorporated nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *******************************************************************************
 * 
 *                       MSP430 CODE EXAMPLE DISCLAIMER
 *
 * MSP430 code examples are self-contained low-level programs that typically
 * demonstrate a single peripheral function or device feature in a highly
 * concise manner. For this the code may rely on the device's power-on default
 * register values and settings such as the clock configuration and care must
 * be taken when combining code from several examples to avoid potential side
 * effects. Also see www.ti.com/grace for a GUI- and www.ti.com/msp430ware
 * for an API functional library-approach to peripheral configuration.
 *
 * --/COPYRIGHT--*/
//******************************************************************************
//   MSP430FR69xx Demo - Long word writes to FRAM
//
//   Description: Use long word write to write to 512 byte blocks of FRAM.
//   Toggle LED after every 100 writes.
//   NOTE: Running this example for extended periods will impact the FRAM
//   endurance.
//   MCLK = SMCLK = default DCO
//
//           MSP430FR6989
//         ---------------
//     /|\|               |
//      | |               |
//      --|RST            |
//        |               |
//        |               |
//        |          P1.0 |---> LED
//
//   William Goh
//   Texas Instruments Inc.
//   April 2014
//   Built with IAR Embedded Workbench V5.60 & Code Composer Studio V6.0
//******************************************************************************
#include <msp430fr6989.h>
#include <stdint.h>
#include "../printf.h"

void FRAMWrite(void);

unsigned int count = 0;
unsigned long *FRAM_write_ptr;
unsigned long data;

#define FRAM_TEST_START 0xD000

int main(void)
{
  WDTCTL = WDTPW | WDTHOLD;                 // Stop WDT

  // Maybe this will unlock the fram controller?
  FRCTL0 = FRCTLPW >> 8;
  GCCTL0 |= FRPWR;

  // Configure GPIO
  P1OUT &= ~BIT0;                           // Clear P1.0 output latch for a defined power-on state
  P1DIR |= BIT0;                            // Set P1.0 to output direction

  // Disable the GPIO power-on default high-impedance mode to activate
  // previously frctlpwconfigured port settings
  PM5CTL0 &= ~LOCKLPM5;
  
  siren_command("PRINTF: GCCTL0 with just pwr %i\n", GCCTL0);

  GCCTL0 |= FRLPMPWR;
  siren_command("PRINTF: GCCTL0 with also lpmpwr %i\n", GCCTL0);

  GCCTL0 &= ~FRPWR;
  siren_command("PRINTF: GCCTL0 after removing pwr bit %i\n", GCCTL0);
  
  GCCTL0 &= ~FRLPMPWR;
  siren_command("PRINTF: GCCTL0 after removing lpmpwr bit as well %i\n", GCCTL0);

  int i;
  for (i = 0; i < 10000; i++);
  FRCTL0 = 0;
  siren_command("PRINTF: Try to write after locking registers...\n");
  GCCTL0 |= FRLPMPWR;

  siren_command("PRINTF: Beginning FRAM test...\n\n");

  // Initialize dummy data
  data = 0x00010001;
  while(1)
  {
    data += 0x00010001;
    FRAM_write_ptr = (unsigned long *)FRAM_TEST_START;
    //mspsim_printf("START: %n\n", FRAM_TEST_START);
    //mspsim_printf("PTR Start: %n\n\n", FRAM_write_ptr+1);
    FRAMWrite();                            // Endless loop
    count++;
    //mspsim_printf("hello\n");
    if (count > 100)
    {
      P1OUT ^= 0x01;                        // Toggle LED to show 512K bytes
      count = 0;                            // ..have been written
      siren_command("PRINTF: FRAMWrite address before write = [%x]\n", FRAM_write_ptr - 1);
      siren_command("PRINTF: FRAMWrite ptr value: %n\n", *(FRAM_write_ptr - 1)); // Subtract one because the above increments it
      //data = 0x00010001;
    } 
  }
}

void FRAMWrite(void)
{
  volatile unsigned int i=0;

  for ( i= 0; i<128; i++)
  {
    //mspsim_printf("FRAMWrite address before write = [%x]\n", FRAM_write_ptr);
    *FRAM_write_ptr++ = data;
    //mspsim_printf("FRAMWrite address after write = [%x]\n", (FRAM_write_ptr - 1));
    //mspsim_printf("FRAMWrite address after increment = [%x]\n", FRAM_write_ptr);
    //mspsim_printf("FRAMWrite ptr value: %n\n", *(FRAM_write_ptr - 1)); // Subtract one because the above increments it
    //mspsim_printf("Data value: %n\n", data);
  }
  //mspsim_printf("FRAMWrite address after loop = [%x]\n", FRAM_write_ptr-1);
  //mspsim_printf("FRAMWrite ptr value after loop: %n\n", *(FRAM_write_ptr - 1)); // Subtract one because the above increments it
  //mspsim_printf("Data value after loop: %n\n", data);
}
