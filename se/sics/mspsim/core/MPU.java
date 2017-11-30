/**
 * This MPU implementation can protect ALL memory addresses, including SRAM, FRAM, and registers, unlike the actual implementation which only protects FRAM. 
 * We rely on user firmware to respect this constraint, or not.
 * 
 * @author Josiah Hester
 */
package se.sics.mspsim.core;

import se.sics.mspsim.core.MPU.MemoryOperation;
import se.sics.mspsim.util.Utils;

public class MPU extends IOUnit {
	// Register addresses
	public static final int MPUCTL0            = 0x05A0;
	public static final int MPUCTL1            = 0x05A2;
	public static final int MPUSEGB2           = 0x05A4;
	public static final int MPUSEGB1           = 0x05A6;
	public static final int MPUSAM             = 0x05A8;
	public static final int MPUIPC0            = 0x05AA;
	public static final int MPUIPSEGB2         = 0x05AC;
	public static final int MPUIPSEGB1         = 0x05AE;
	
	// Masks
	private static final int MPUENA            = 0x0001;       /* MPU Enable */
	private static final int MPULOCK           = 0x0002;       /* MPU Lock */
	private static final int MPUSEGIE          = 0x0010;       /* MPU Enable NMI on Segment violation */
	private static final int MPUPW             = 0xA5;       /* MPU Enable NMI on Segment violation */
	private static final int MPUSEG1WE         = 0x0002;       /* MPU Main memory Segment 1 Write enable */
	private static final int MPUSEG1RE         =    0x0001;       /* MPU Main memory Segment 1 Read enable */
	private static final int MPUSEG1XE         =    0x0004;       /* MPU Main memory Segment 1 Execute enable */
	private static final int MPUSEG2RE         =    0x0010;       /* MPU Main memory Segment 2 Read enable */
	private static final int MPUSEG2WE         =    0x0020;       /* MPU Main memory Segment 2 Write enable */
	private static final int MPUSEG2XE         =    0x0040;       /* MPU Main memory Segment 2 Execute enable */
	private static final int MPUSEG3RE         =    0x0100;       /* MPU Main memory Segment 3 Read enable */
	private static final int MPUSEG3WE         =    0x0200;       /* MPU Main memory Segment 3 Write enable */
	private static final int MPUSEG3XE         =    0x0400;       /* MPU Main memory Segment 3 Execute enable */
	private static final int MPUSEGIRE         =    0x1000;       /* MPU Info memory Segment Read enable */
	private static final int MPUSEGIWE         =    0x2000;       /* MPU Info memory Segment Write enable */
	private static final int MPUSEGIXE         =    0x4000;       /* MPU Info memory Segment Execute enable */
	
	// Default register values
	int mpuctl0            = 0x9600;
	int mpuctl1            = 0x0;
	int mpusegb2           = 0x0;
	int mpusegb1           = 0x0;
	int mpusam             = 0x7777;
	int mpuipc0            = 0x0;
	int mpuipsegb2         = 0x0;
	int mpuipsegb1         = 0x0;
	
	enum MemoryOperation {
		WRITE, READ, EXECUTE
	}
	
	boolean registerAccessEnabled = false;
	boolean mpuEnabled = false;
	boolean mpuLocked = false;
	boolean mpuIntEnabled = false;
	
    public MPU(String id, MSP430Core cpu, int[] memory, int offset) {
        super(id, cpu, memory, offset);        
    }
    
    public MPUOperationResult  isValidOperation(int address, MemoryOperation op) {
    	MPUOperationResult  mpuop = new MPUOperationResult();
    	mpuop.address = address;
    	mpuop.op = op;
    	boolean error = false;
    	// Segment 1
    	if(address < (mpusegb1 << 4)) {
    		mpuop.segment = 1;
    		if(op == MemoryOperation.EXECUTE && (MPUSEG1XE & mpusam) == 0) error = true;
    		if(op == MemoryOperation.WRITE && (MPUSEG1WE & mpusam) == 0) error = true;
    		if(op == MemoryOperation.READ && (MPUSEG1RE & mpusam) == 0) error = true;
    	} else // Segment 2
    	if(address >= (mpusegb1 << 4) && address < (mpusegb2 << 4)) {
    		mpuop.segment = 2;
    		if(op == MemoryOperation.EXECUTE && (MPUSEG2XE & mpusam) == 0) error = true;
    		if(op == MemoryOperation.WRITE && (MPUSEG2WE & mpusam) == 0) error = true;
    		if(op == MemoryOperation.READ && (MPUSEG2RE & mpusam) == 0) error = true;
    	} else	// Segment 3
    	if(address >= (mpusegb2 << 4)) {
    		mpuop.segment = 3;
    		if(op == MemoryOperation.EXECUTE && (MPUSEG3XE & mpusam) == 0) error = true;
    		if(op == MemoryOperation.WRITE && (MPUSEG3WE & mpusam) == 0) error = true;
    		if(op == MemoryOperation.READ && (MPUSEG3RE & mpusam) == 0) error = true;
    	}
    	mpuop.error = error;
    	return mpuop;
    }
    
    public void badOperation(MPUOperationResult op) {
    	// Set flags for access violation
    	mpuctl1 |= (1 << (op.segment-1));
    }
    
	@Override
	public void interruptServiced(int vector) {
		System.out.println("MPU: Interrupt serviced");

	}
	
	@Override
	public void reset(int type) {
		mpuctl0            = 0x9600;
		mpuctl1            = 0x0;
		mpusegb2           = 0x0;
		mpusegb1           = 0x0;
		mpusam             = 0x7777;
		mpuipc0            = 0x0;
		mpuipsegb2         = 0x0;
		mpuipsegb1         = 0x0;
		super.reset(type);
	}
	@Override
	public void write(int address, int value, boolean word, long cycles) {
		 if (DEBUG) log("MPU write to: " + Utils.hex(address, 4) + ": " + value);
	     switch (address) {
	        case MPUCTL0:
	        	mpuctl0 = value;
	        	// MPUPW
	        	if((value >> 8) == MPUPW) {
	        		registerAccessEnabled = true;
	        	} else {
	        		if (DEBUG) log("MPU password wrong, PUC triggered: " + Utils.hex(address, 4) + ": " + value);
	        		reset(0);
	        	}
	        	if((MPUENA & value) ==  1) {
	        		mpuEnabled = true;
	        	} else {
	        		mpuEnabled = false;
	        	}
	        	if((MPULOCK & value) > 0) {
	        		mpuLocked = true;
	        	} else {
	        		mpuLocked = false;
	        	}
	        	if((MPUSEGIE & value) > 0) {
	        		mpuIntEnabled = true;
	        	} else {
	        		mpuIntEnabled = false;
	        	}
	        	
	        	break;
	        case MPUCTL1:
	        	mpuctl1 = value;
	        	break;
	        case MPUSEGB2:
	        	mpusegb2 = value;
	        	break;
	        case MPUSEGB1:
	        	mpusegb1 = value;
	        	break;
	        case MPUSAM:
	        	mpusam = value;
	        	break;
	        case MPUIPC0:
	        	mpuipc0 = value;
	        	break;
	        case MPUIPSEGB2:
	        	mpuipsegb2 = value;
	        	break;
	        case MPUIPSEGB1:
	        	mpuipsegb1 = value;
	        	break;
	     }

	}

	@Override
	public int read(int address, boolean word, long cycles) {
		int value = 0x0; 
	     switch (address) {
	        case MPUCTL0:
	        	value = mpuctl0;
	        	break;
	        case MPUCTL1:
	        	value = mpuctl1;
	        	// reset by reading
	        	mpuctl1 = 0x0;
	        	break;
	        case MPUSEGB2:
	        	value = mpusegb2;
	        	break;
	        case MPUSEGB1:
	        	value = mpusegb1;
	        	break;
	        case MPUSAM:
	        	value = mpusam;
	        	break;
	        case MPUIPC0:
	        	value = mpuipc0;
	        	break;
	        case MPUIPSEGB2:
	        	value = mpuipsegb2;
	        	break;
	        case MPUIPSEGB1:
	        	value = mpuipsegb1;
	        	break;
	     }
	    if (DEBUG) log("MPU read at: " + Utils.hex(address, 4) + ": " + value);
		return value;
	}

}

class MPUOperationResult {
	int segment;
	MemoryOperation op;
	int address;
	boolean error; 
}

class MPUSegment {
	boolean read, write, execute;
	int boundstart, boundsend;
	
}