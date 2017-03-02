package se.sics.mspsim.core.eusci;

import se.sics.mspsim.core.DMATrigger;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.USARTSource;

public abstract class EUSCI extends IOUnit implements DMATrigger, USARTSource {

	// USCI A and B common register offsets
    protected static final int CTL0 = 0x00; 
    protected static final int CTL1 = 0x02;
    protected static final int BR0 = 0x06;
    protected static final int BR1 = 0x07;
    protected static final int RXBUF = 0x0c;
    protected static final int TXBUF = 0x0e;
    
    // Misc flags
    protected static final int RXIFG = 0x01;
    protected static final int TXIFG = 0x02;
        
    protected static final int USCI_BUSY = 0x01;

    protected static final int SWRST = 0x01;
    
	public EUSCI(String id, MSP430Core cpu, int[] memory, int offset) {
		super(id, cpu, memory, offset);
	}
}
