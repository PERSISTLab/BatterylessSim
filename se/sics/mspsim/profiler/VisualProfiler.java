package se.sics.mspsim.profiler;

import java.io.PrintStream;
import java.util.HashMap;

import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.StopExecutionException;
import se.sics.mspsim.profiler.CallEntry.CallCounter;
import se.sics.mspsim.util.MapEntry;
import se.sics.mspsim.util.Utils;

public class VisualProfiler extends SimpleProfiler {

	private VisualProfilerGUI gui;
	private boolean nextfunc;
	
	public static enum STATE {
		INFUNCTION,
		LEAVEFUNCTION;
	};

	public VisualProfiler() {
		gui = VisualProfilerGUI.getInstance();
		nextfunc = false;
	}
	
	public void setNextFunc (boolean nextfunc) {
		this.nextfunc = nextfunc;
	}
	
	public void profileCall(MapEntry entry, long cycles, int from) {
		if (cSP == callStack.length) {
			CallEntry[] tmp = new CallEntry[cSP + 64];
			System.arraycopy(callStack, 0, tmp, 0, cSP);
			callStack = tmp;
		}
		if (callStack[cSP] == null) {
			callStack[cSP] = new CallEntry();
		}

		// XXX hard-coded hackery
		if ("__mementos_checkpoint".equals(entry.getName())) {
			cpu.inCheckpoint = true;
			cyclesAtStartOfCheckpoint = cpu.cycles;
		}

		int hide = 0;
		PrintStream logger = this.logger;
		if (logger != null) {
			/* hide this if last call was to be hidden */
			hide = (cSP == 0 || newIRQ) ? 0 : callStack[cSP - 1].hide;
			/* increase level of "hide" if last was hidden */
			if (hide > 0) hide++;
			if ((!hideIRQ || servicedInterrupt == -1) && hide == 0) {
				if (servicedInterrupt >= 0) logger.printf("[%2d] ", servicedInterrupt);
				printSpace(logger, (cSP - interruptLevel) * 2);
				logger.println("Call to $" + Utils.hex(entry.getAddress(), 4) +
						": " + entry.getInfo() + " R15: " + Utils.hex16(cpu.readRegister(15)));
								
				boolean next = gui.colorVertex(entry.getName(), VisualProfiler.STATE.INFUNCTION);
				shouldStop(next);
				
				if (ignoreFunctions.get(entry.getName()) != null) {
					hide = 1;
				}
			}
		}


		CallEntry ce = callStack[cSP++];
		ce.function = entry;
		ce.calls = 0;
		ce.cycles = cycles;
		ce.exclusiveCycles = cycles;
		ce.hide = hide;
		ce.fromPC = from;
		newIRQ = false;


		if (stackMonitor != null) {
			/* get the current stack MAX for previous function */
			if (cSP > 1) {
				callStack[cSP - 2].currentStackMax = stackMonitor.getProfStackMax();
			}
			/* start stack here! */
			ce.stackStart = stackMonitor.getStack();
			stackMonitor.setProfStackMax(stackMonitor.getStack());
		}

		CallListener[] listeners = callListeners;
		if (listeners != null) {
			for (int i = 0, n = listeners.length; i < n; i++) {
				listeners[i].functionCall(this, ce);
			}
		}
	}
	
	public void profileReturn(long cycles) {
		boolean shouldStopExecution = false;
		if (cSP <= 0) {
			/* the stack pointer might have been messed with? */
			PrintStream logger = this.logger;
			if (logger != null) {
				// logger.println("SimpleProfiler: Too many returns?");
			} else {
				// System.err.println("SimpleProfiler: Too many returns?");
			}
			return;
		}
		CallEntry cspEntry = callStack[--cSP];
		MapEntry fkn = cspEntry.function;
		//	     System.out.println("Profiler: return / call stack: " + cSP + ", " + fkn);

		long elapsed = cycles - cspEntry.cycles;
		long exElapsed = cycles - cspEntry.exclusiveCycles;
		if (cSP != 0) {
			callStack[cSP-1].exclusiveCycles += exElapsed;
		}
		int maxUsage = 0;

		if (elapsed < 0) {
			System.err.println("elapsed < 0!  cycles=" + cycles + "; "
					+ "cspEntry.cycles=" + cspEntry.cycles + "; ");
			printStackTrace(System.err);
		}

		if (cspEntry.calls >= 0) {
			CallEntry ce = profileData.get(fkn);
			if (ce == null) {
				profileData.put(fkn, ce = new CallEntry());
				ce.function = fkn;
			}
			ce.cycles += elapsed;
			ce.exclusiveCycles += exElapsed;
			ce.calls++;

			if (stackMonitor != null) {
				maxUsage = stackMonitor.getProfStackMax() - cspEntry.stackStart;
				ce.stackStart = cspEntry.stackStart;
				if (maxUsage > ce.currentStackMax) {
					ce.currentStackMax = maxUsage;
				}
				if (cSP != 0) {
					/* put the max for previous function back into the max profiler */ 
					stackMonitor.setProfStackMax(callStack[cSP-1].currentStackMax);
				}
			}



			if (cSP != 0) {
				MapEntry caller = callStack[cSP-1].function;
				HashMap<MapEntry,CallCounter> callers = ce.callers;
				CallCounter numCalls = callers.get(caller);
				if (numCalls == null) {
					numCalls = new CallCounter();
					callers.put(caller, numCalls);
				}
				numCalls.count++;
			}

			// XXX hard-coded hackery
			if ("__mementos_checkpoint".equals(ce.function.getName())) {
				cpu.inCheckpoint = false;
			} else if ("__mementos_restore".equals(ce.function.getName())) {
				shouldStopExecution = true;
			}

			PrintStream logger = this.logger;
			if (logger != null) {
				if ((cspEntry.hide <= 1) && (!hideIRQ || servicedInterrupt == -1)) {
					if (servicedInterrupt >= 0) logger.printf("[%2d] ",servicedInterrupt);
					printSpace(logger, (cSP - interruptLevel) * 2);
					logger.println("return from " + ce.function.getInfo() + " elapsed: " + elapsed + " maxStackUsage: " + maxUsage + " R15: " + Utils.hex16(cpu.readRegister(15)));
					//gui.removeLastNode();
					boolean next = gui.colorVertex(ce.function.getName(), VisualProfiler.STATE.LEAVEFUNCTION);
					shouldStop(next);
				}
			}

			CallListener[] listeners = callListeners;
			if (listeners != null) {
				for (int i = 0, n = listeners.length; i < n; i++) {
					listeners[i].functionReturn(this, cspEntry);
				}
			}
		}
		newIRQ = false;
		if (shouldStopExecution) {
			throw new StopExecutionException(cpu.readRegister(15), "return from __mementos_restore");
		}
	}
	
	@Override
	public void profileInterrupt(MapEntry entry, int vector, long cycles) {
		servicedInterrupt = vector;
		interruptFrom = cpu.getPC(); 
		lastInterruptTime[servicedInterrupt] = cycles;
		interruptLevel = cSP;
		newIRQ = true;

		PrintStream logger = this.logger;
		if (logger != null && !hideIRQ) {
			logger.println("----- Interrupt vector " + vector + " start execution " +
					"(@cycle " + cpu.cycles +") -----");
			boolean next = gui.colorVertex(entry.getName(), VisualProfiler.STATE.INFUNCTION);
			shouldStop(next);
		}
	}
	
	@Override
	public void profileRETI(MapEntry entry, long cycles) {
		if (servicedInterrupt > -1) {
			interruptTime[servicedInterrupt] += cycles - lastInterruptTime[servicedInterrupt];
			interruptCount[servicedInterrupt]++;
		}
		newIRQ = false;

		PrintStream logger = this.logger;
		if (logger != null && !hideIRQ) {
			logger.println("----- Interrupt vector " + servicedInterrupt + " returned - elapsed: " +
					(cycles - lastInterruptTime[servicedInterrupt]));
			boolean next = gui.colorVertex(entry.getName(), VisualProfiler.STATE.LEAVEFUNCTION);
			shouldStop(next);
		}
		interruptLevel = 0;

		/* what if interrupt from interrupt ? */
		servicedInterrupt = -1;
	}
	
	private void shouldStop (boolean next) {
		if (nextfunc) {
			if (next) {
				if (cpu instanceof MSP430) {
					((MSP430)cpu).stop();
				} else {
					System.out.println("This profiling only works with MSP430 cpus!");
				}
			}
		}
	}
}
