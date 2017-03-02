package se.sics.mspsim.core;

import se.sics.mspsim.core.EmulationLogger.WarningType;

public class Fram implements Memory {

	private final MSP430Core core;
    private final int memory[];
    private final FramController fram;

    public Fram(MSP430Core core, FramController fram) {
        this.core = core;
        this.memory = core.memory;
        this.fram = fram;
    }
    
	@Override
	public int read(int address, AccessMode mode, AccessType type) throws EmulationException {

	    // Anytime there is a read-or-write from fram, the FRPWR bit is set to high
		fram.setFRPWRHigh();
		
		int val = memory[address] & 0xff;
        if (mode != AccessMode.BYTE) {
            val |= (memory[address + 1] & 0xff) << 8;
            if ((address & 1) != 0) {
                core.printWarning(WarningType.MISALIGNED_READ, address);
            }
            if (mode == AccessMode.WORD20) {
                /* will the read really get data from the full word? CHECK THIS */
                val |= (memory[address + 2] & 0xf) << 16;
            }
        }
        return val;
	}

	@Override
	public void write(int dstAddress, int data, AccessMode mode) throws EmulationException {
	    // Anytime there is a read-or-write from fram, the FRPWR bit is set to high
		fram.setFRPWRHigh();
		
		fram.framWrite(dstAddress, data, mode);
	}

	@Override
	public int get(int address, AccessMode mode) {
		
		int val = memory[address] & 0xff;
        if (mode != AccessMode.BYTE) {
            val |= (memory[address + 1] << 8);
            if ((address & 1) != 0) {
                core.printWarning(WarningType.MISALIGNED_READ, address);
            }
            if (mode == AccessMode.WORD20) {
                /* will the read really get data from the full word? CHECK THIS */
                val |= (memory[address + 2] << 16) | (memory[address + 3] << 24);
                val &= 0xfffff;
            } else {
                val &= 0xffff;
            }
        }
        return val;
	}

	@Override
	public void set(int address, int data, AccessMode mode) {
		write(address, data, mode);	
	}

}
