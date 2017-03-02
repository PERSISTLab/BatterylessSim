package se.sics.mspsim.util;
import se.sics.mspsim.platform.GenericNode;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

import edu.umass.energy.Capacitor;

public class CheckpointValidator
{
	protected boolean active = false;
	protected boolean inChk = false;
	
	protected int f1start;
	protected int f2start;

	protected int cpfunc;
	protected int global_size;

	protected int[] preregs;
	protected int[] premem;

    protected int calldepth = 0;

    private GenericNode node;
    private String logfile;
    protected static int bundleCounter = 0;
    protected final int MAGIC_NUMBER = 0xBEAD;
    
    public static final int SEGMENT_A = 0xF900;
    public static final int SEGMENT_B = 0xFB00;
    public static final int SEGMENT_SIZE = 512;

    private long cyclesAtEntry = 0;
    private long cyclesAtEndOfLastFullCheckpoint = 0;
    
    private boolean isOracleCall = false;

	
public CheckpointValidator(GenericNode node)
{
    this.node = node;
	active = false;
}

public void init(int f1, int f2, int func, int gsize, String logfile)
{
	f1start = f1;
	f2start = f2;
	cpfunc = func;
	global_size = gsize;

    if (null != logfile) {
    	// timestamp this run with the current time in milliseconds
        this.logfile = logfile + "." + System.currentTimeMillis();
    }

	inChk = false;
	active = true;
}

public void reset () {
	inChk = false;
	calldepth = 0;
	cyclesAtEntry = cyclesAtEndOfLastFullCheckpoint = 0;
}

public boolean isActive()
{
	return active;
}

public boolean isInChk()
{
	return inChk;
}

public int getCPFunc()
{
	if (active)
	{
		return cpfunc;
	} else {
		return 0xFFFFFF;
	}
}

public void pushFunCall()
{
    ++calldepth;
}

public void pushFunCall(boolean isOracleCall) {
	this.isOracleCall = isOracleCall;
	pushFunCall();
}

public int popFunCall()
{
    return --calldepth;
}

public void preCall(int[] regs, int[] memory, long cycles)
{
	preregs = new int[regs.length];
	for (int i=0; i < regs.length; i++)
	{
		preregs[i] = regs[i];
	}
    preregs[1] += 2; // hack to account for implicit push of retaddr onto stack

	premem = new int[memory.length];
	for (int i=0; i < memory.length; i++)
	{
		premem[i] = memory[i];
	}
	inChk = true;

    cyclesAtEntry = cycles;
}

/**
 * Called when the registered checkpoint function returns.
 * @param regs The register file from the CPU
 * @param memory The CPU's memory
 * @param stackStartAddr The address of the beginning of the CPU's stack
 * @param cycles The CPU's current cycle counter when this method is called 
 * @return false if the checkpoint function wrote a bundle that fails to validate
 */
public boolean postCall(int[] regs, int[] memory, int stackStartAddr, long cycles)
{
	int i;
	boolean result =true;
	inChk = false;

    // heuristic: did checkpoint do anything expensive?
    if (cycles - cyclesAtEntry < (Capacitor.ADC_CYCLES + 100/*fudge*/)) {
        return true; // ok by default
    }
    cyclesAtEntry = 0;

	//get bundle pointer
    int addr = findActiveBundlePointer(memory);

    if (addr == 0xFFFF) { // nothing to check
        System.err.println("Bundle pointer " + addr + " is 0xFFFF; no bundle to check");
        return true;
    } else {
        System.err.println("Active bundle at " + Utils.hex16(addr));
    }

	//TODO: compare stored checkpoint with previous registers and memory.
	int globalssize = memory[addr];
	int stacksize = memory[addr+1];
    int regstart = addr+2;
    int stackstart = regstart + 30; //15 registers * 2 bytes
    int globalstart = stackstart + stacksize;
    int magicnum = globalstart + globalssize;

    if (null != logfile) {
        String logfileName = logfile + "." + bundleCounter++;
        try {
            PrintWriter logwriter =
                new PrintWriter(new BufferedWriter(new FileWriter(logfileName,
                                true)));
            int end = addr
                + stacksize + globalssize
                + 30 // registers
                + 2  // bundle header
                + 2; // magic number
            for (i = addr; i < end; i+=2) {
                logwriter.println(Utils.hex16(memory[i] | (memory[i+1]<<8)));
            }
            logwriter.println("==========");
            logwriter.println("Stack: " + stacksize + " bytes");
            logwriter.println("Globals: " + globalssize + " bytes");
            logwriter.println("R0(PC): " +
                    Utils.hex16(memory[regstart] | (memory[regstart+1] << 8)));
            logwriter.println("R1(SP): " +
                    Utils.hex16(memory[regstart+2] | (memory[regstart+3] << 8)));
            logwriter.println("R2(SR): " +
                    Utils.hex16(memory[regstart+4] | (memory[regstart+5] << 8)));
            for (int a = regstart + 6; a < stackstart; a += 2) {
                logwriter.println("R" + (((a - regstart) / 2) + 1) + ": " +
                        Utils.hex16(memory[a] | (memory[a+1] << 8)));
            }
            logwriter.close();
        } catch (IOException ioe) {
            System.err.println("Logfile '" + logfile + "' error:" +
                    ioe.getMessage());
            node.stop();
        }
    }

    //check magic number
    if ((memory[magicnum] | (memory[magicnum+1] << 8)) != MAGIC_NUMBER) {
        System.err.println("Magic number does not validate!");
        result = false;
    }

	//compare registers
    int j;
	for (i=0; i < 16; i++)
	{
        // skip over R3
        if (i == 3) continue;
        j = (i > 3) ? i-1 : i;

        int radr = regstart + (j * 2);
        int rval = memory[radr] | (memory[radr+1] << 8);

		if (rval !=  preregs[i])
		{
			if (i == 0) {
				if (isOracleCall && (rval != preregs[i]+2)) {
					result = false;
				}
			} else if (i == 1) {
				if (isOracleCall && (rval != preregs[i]-2)) {
					result = false;
				}
			} else if (i == 2) {
                System.err.println("Register R2 mismatch, but that's OK");
            } else {
                System.err.println("Register R"+i+" does not match checkpt (0x"
                        + Utils.hex16(preregs[i])
                        + " != 0x" + Utils.hex16(rval) + ")");
                result = false;
            }
		}
	}

	//compare stack
	for (i=0; i < stacksize; i+=2)
	{
		//previous snapshot address and value
		int sadr = (stackStartAddr - stacksize) + i;
		int sval = premem[sadr] | (premem[sadr+1] << 8);

		//saved checkpoint address and value
		int chk_sadr = stackstart + i;
		int chk_sval = memory[chk_sadr] | (memory[chk_sadr+1] << 8);

		if (sval !=  chk_sval)
		{
			System.err.println("Stack difference at offset="+i+" ("+
                    Utils.hex16(sval)+" @ $" + Utils.hex16(sadr) + " != " +
                    Utils.hex16(chk_sval) +" @ $"+ Utils.hex16(chk_sadr) + ")");
			result = false;
		}
	}

	//compare globals
	if (globalssize != global_size)
	{
		System.err.println("Global data sizes don't agree ("+globalssize+" != "+global_size+")");
		result = false;
	}

    if (result) {
    	System.err.println("Checkpoint at $"+Utils.hex16(addr)+" validated OK!");
    	cyclesAtEndOfLastFullCheckpoint = cycles;
    } else {
        System.err.println("Bad checkpoint at $"+Utils.hex16(addr));
        node.stop();
    }
	return result;
}

public static boolean segmentIsEmpty (int[] memory, int seg) {
    int a;
    int stride = 32;
    for (a = seg; a < seg+SEGMENT_SIZE; a += stride) {
        if (readWord(memory, a) != 0xFFFF) return false;
    }
    return true;
}

public static boolean segmentIsMarkedForErasure (int[] memory, int seg) {
    return ((readWord(memory, seg+2) == 0) &&
    		(readWord(memory, seg+SEGMENT_SIZE-2) == 0));
}

public static int readWord (int[] memory, int addr) {
    return (memory[addr] | (memory[addr+1] << 8));
}


/**
  * basically copied from newmem.c r1078 */
public int findActiveBundlePointer (int[] memory) {
    int bun = SEGMENT_A;
    int candidate = 0xFFFF;
    boolean segAerase = false;

    /* if both segments are marked for erasure, no checkpoints; bail */
    if (segmentIsMarkedForErasure(memory, SEGMENT_A)) {
        System.err.println("Bundle segment A is marked for erasure");
        segAerase = true;
    }
    if (segmentIsMarkedForErasure(memory, SEGMENT_B)) {
        System.err.println("Bundle segment B is marked for erasure");
        if (segAerase) return candidate;
    }

    if (segmentIsEmpty(memory, SEGMENT_A) ||
    		segmentIsMarkedForErasure(memory, SEGMENT_A)) {
        bun = SEGMENT_B;
        do {
            if (readWord(memory, bun) == 0xFFFF)
                return candidate;

            int endloc = bun + (readWord(memory, bun) & 0xff)
                + (readWord(memory, bun) >> 8) + 2 + 30;
            int magic = readWord(memory, endloc);
            if (magic == MAGIC_NUMBER) {
                candidate = bun;
                System.err.println("Candidate @ " + Utils.hex16(candidate)
                        + ": " + getBundleString(memory, candidate));
            }
            bun = endloc + 2;
        } while (bun < SEGMENT_B + SEGMENT_SIZE);
        return candidate;
    }

    do {
        if (readWord(memory, bun) == 0xFFFF)
            return candidate;

        int endloc = bun + (readWord(memory, bun) & 0xff)
            + (readWord(memory, bun) >> 8) + 2 + 30;
        int magic = readWord(memory, endloc);
        if (magic == MAGIC_NUMBER) {
            candidate = bun;
            System.err.println("Candidate @ " + Utils.hex16(candidate)
                    + ": " + getBundleString(memory, candidate));
        }
        bun = endloc + 2;
    } while (bun < SEGMENT_A + SEGMENT_SIZE);
    return candidate;
}

public long getCyclesAtEndOfLastFullCheckpoint() {
	return cyclesAtEndOfLastFullCheckpoint;
}

public String getBundleString(int[] memory, int addr) {
    StringBuilder sb = new StringBuilder();
	int globalssize = memory[addr];
	int stacksize = memory[addr+1];
    int end = addr
        + stacksize + globalssize
        + 30 // registers
        + 2  // bundle header
        + 2; // magic number
    for (int i = addr; i < end; i+=2)
        sb.append(Utils.hex16(memory[i] | (memory[i+1]<<8)));
    return sb.toString();
}

}
