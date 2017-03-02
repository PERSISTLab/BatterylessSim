#include <msp430fr6989.h>
#include "cc1101.h"
#include <string.h>
#include "packet.h"
#include <stdint.h>
#include "../../../lib/printf.h"

uint8_t tx_buffer[61]={0};
uint8_t rx_buffer[61]={0};
volatile uint8_t burstnum,i,flag, sizerx;

/*#ifndef DEVICEID
	DEVICEID=1
#endif*/
int DEVICEID = 1;	

int main(void){
	//Stop watchdog timer
	WDTCTL = WDTPW + WDTHOLD;
	
	//PJDIR |= BIT1 + BIT0;
	//PJOUT &= ~(BIT1 + BIT0);
	
//	PJSEL0 &= ~BIT1;
//	PJSEL1 &= ~BIT1;	

	// Create a packet to send, fill it out, then encode it in the buffer
	sync_t pkt;
	pkt = create_sync_packet(DEVICEID, 0, 1000);
	
	//delay(1);
	Radio.Init();	
	Radio.SetDataRate(5); // Needs to be the same in Tx and Rx
	Radio.SetLogicalChannel(1);
	Radio.SetTxPower(3);
	while(1) {	
		memcpy(tx_buffer, &pkt, sizeof(pkt));
		Radio.SendData(tx_buffer,sizeof(pkt));
		//PJOUT |= BIT1;		
		Radio.RxOn(); // receive mode active
		//while(!Radio.CheckReceiveFlag());	// if buffer has contents then flag returns true.	
		//sizerx=Radio.ReceiveData(rx_buffer); // put contents into RX buffer
		//mspsim_printf("sizerx=%i\n", sizerx);
		// Decode the packet
		/*sync_t tmp = decode_sync_packet(rx_buffer);
		PJOUT &= ~BIT1;	// Toggle light LED
		*/
		Radio.Idle();		
		pkt.local_time += 10;
		delay(50);
	}
	return 1;
}
