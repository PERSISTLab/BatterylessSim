package edu.umass.energy;
import java.util.HashMap;
import java.util.Map;

import se.sics.mspsim.chip.Peripheral;
import se.sics.mspsim.core.ADCInput;
import se.sics.mspsim.core.FramController;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.core.StopExecutionException;

import edu.clemson.ekho.Ekho;
import edu.clemson.ekho.IVSurfaceEndedException;
import edu.clemson.eval.EvalLogger;

public class Capacitor extends IOUnit implements ADCInput, PortListener {
	
	private Map<Peripheral, ResistanceState> peripheralMap = new HashMap<>();
    private double capacitance;
    private double voltage;
    private double effectiveMaxVoltage;
    private double inputVoltageDividerFactor;
    private double inputVoltageReferenceVoltage;
    private double A; // _The Art of Electronics_, 2nd Ed., p. 23
    private double lastATime = 0; // when was A last set?
    
    private static final double meaninglessStartTime = -1.0;
    private double startTime = meaninglessStartTime;
    private MSP430 cpu;
    private double initialVoltage;
    private long numSetVoltages = 0;
    private long numUpdateVoltages = 0;
    public long accumCycleCount = 0;
    private long numLifecycles = 1;
    private long printCounter = 0;
    private int powerMode;
    private boolean suppressVoltagePrinting = false;
    private boolean suppressEnergyPrinting = false;
    public EnergyFairy eFairy;
    public Ekho ekhoFairy;
    private EvalLogger ekhologger;

    private boolean enabled = true;

    public static final int POWERMODE_OFF = -1;
    public static final int POWERMODE_ACTIVE = 0;
    public static final int POWERMODE_LPM0 = 1;
    public static final int POWERMODE_LPM1 = 2;
    public static final int POWERMODE_LPM2 = 3;
    
    public static final int POWERMODE_LPM3 = 4;
    public static final int POWERMODE_LPM4 = 5;
    public static final int POWERMODE_FLWRI = 100;
    public static final int POWERMODE_ADC = 101;
    
    public static final int ADC_CYCLES = 647;

    /* Spreadsheety-looking comments in this block refer to the Google
     * spreadsheet at
     * https://spreadsheets.google.com/ccc?key=0AjDLMKJ2t5rLdFFRME5CeTQ1WkNHMEV5UmVyTi1XVHc&hl=en
     * Furthermore, resistance numbers here refer to 4.5V numbers.  Units are
     * ohms.
     public static final double MSP430_RESISTANCE_ADC  =   14504; // E86
     public static final double MSP430_RESISTANCE_ACTIVE = 19007; // E16
     public static final double MSP430_RESISTANCE_FLWRT  = 21505; // E100
     public static final double MSP430_RESISTANCE_LPM0 =   82647; // E72
     public static final double MSP430_RESISTANCE_LPM1 =   82702; // E58
     public static final double MSP430_RESISTANCE_LPM2 =  219560; // E44
     public static final double MSP430_RESISTANCE_LPM3 = 1560000; // E2
     public static final double MSP430_RESISTANCE_LPM4 = 1801200; // E30
     */

    private CapClockSource clockSource;
    // private double defaultResistance = MSP430_RESISTANCE_ACTIVE;
    private double eFairyPrevVoltage = 0.0;
	private double ekhoFairyPrevVoltage = 0.0;
	private double maxEkhoRuntime = 0.0;
	private double ekhoFairyPrevCurrent;
	private static double MIN_CURRENT = 0.0000001; // Can't be zero to start out
    public double resistanceADCRead (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9824
        return (3346.2 * voltage) + 1040.8;
    }
    
    double resistanceOff(double voltage) {
    	// Resistance is only the comparator, so voltage / 1.5uA
    	return voltage / 0.0000015;
    }
    
    double resistanceActive (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9958
        return (4010.6 * voltage) + 803.53;
    }

    public double resistanceFlashWrite (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9998
        return (4747.8 * voltage) + 152.98;
    }

    double resistanceLPM0 (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9999
        return (18232 * voltage) + 1017.9;
    }

    double resistanceLPM1 (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9999
        return (18230 * voltage) + 1014.3;
    }

    double resistanceLPM2 (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.999
        return (48202 * voltage) + 6229.5;
    }

    double resistanceLPM3 (double voltage) {
        // Cubic regression calculated from spreadsheet; R^2 = 0.994
        return
            (-66859 * Math.pow(voltage, 3))
            + (532699 * Math.pow(voltage, 2))
            - (979608 * voltage)
            + 1e6;
    }

    double resistanceLPM4 (double voltage) {
        // Cubic regression calculated from spreadsheet; R^2 = 0.9944
        return
            (-128337 * Math.pow(voltage, 3))
            + (1e6 * Math.pow(voltage, 2))
            - (2e6 * voltage)
            + 3e6;
    }

    /* unused on MSP430F2132, believed unused on MSP430F1611; should be safe to
       inhabit this address. */
    public static final int voltageReaderAddress = 0x01C0;
	private static final double R_ESR = 1000000;//0.001;

    /**
      * @param msp430Core The MSP430 whose livelihood depends on this Capacitor
      * @param C Capacitance in farads, e.g. 10e-6 == 10uF.
      * @param initialVoltage Initial voltage in volts, e.g. 4.5 == 4.5V.
      */
    public Capacitor (MSP430Core msp430Core, double C, double initVoltage,
            double inputVoltageDividerFactor,
            double inputVoltageReferenceVoltage) {
        super("capacitor", msp430Core, msp430Core.memory, voltageReaderAddress);
        cpu = (MSP430) msp430Core;
        capacitance = C;
        this.effectiveMaxVoltage =
            (inputVoltageDividerFactor * inputVoltageReferenceVoltage);
        this.setPowerMode(POWERMODE_OFF);
        setClockSource(msp430Core);
        setInitialVoltage(initVoltage);
    }

    public void setEnergyFairy (EnergyFairy ef) {
        this.eFairy = ef;
    }
    
    public void setEkhoFairy (Ekho ef) {
    	ekhologger = EvalLogger.getInstance(ef.getName());
    	maxEkhoRuntime = ef.getSurfaceEndTime();
    	// evallogger.setFilename(ef.getName()); Does this in constructor?
    	this.ekhoFairy = ef;
    	ekhoFairyPrevCurrent = ekhoFairy.getCurrent(0.0, initialVoltage);
    	if(ekhoFairyPrevCurrent < MIN_CURRENT) ekhoFairyPrevCurrent = MIN_CURRENT;
    }

    public void setInitialVoltage (double initVoltage) {
        this.voltage = initialVoltage = initVoltage;
        setA(initialVoltage, false);
    }

    public void reset () {
        setPowerMode(POWERMODE_ACTIVE);
        startTime = meaninglessStartTime;
        setVoltage(initialVoltage);
        setA(voltage, false);
        numSetVoltages = 0;
        numUpdateVoltages = 0;
        System.err.println("Capacitor.reset(): voltage=" + voltage +
                "; startTime=" + startTime);
    }

    public double getElapsedTimeMillis () {
        return cpu.getTimeMillis() - startTime;
    }

    /**
     * @return Voltage, in volts.
     */
    public double getVoltage () { return voltage; }

    /**
     * Subtracts the given <tt>amount</tt> of energy from this Capacitor,
     * setting and returning the Capacitor's voltage.
     * @param amount Positive amount of energy to subtract, in joules.
     * @return The new voltage value, in volts.
     */
    public void dockEnergy (double amount) {
        /*
        double initialEnergy = 0.5 * capacitance * voltage * voltage;
        double newEnergy = initialEnergy - amount;
        double newVoltage = Math.sqrt(2.0 * newEnergy / capacitance);
        setVoltage(newVoltage);
        */
        // or, more verbosely (and quickly):
        setVoltage(Math.sqrt(2.0 * ((0.5 * capacitance * voltage * voltage) - amount) / capacitance));
    }

    protected void setVoltage (double V) {
            ++numSetVoltages;
            
            voltage = V;
            if(voltage > effectiveMaxVoltage) {
                System.err.println("Voltage exceeds maximum!");
            }
    }

    public long getNumSetVoltages () {
        return numSetVoltages;
    }

    public long getNumUpdateVoltages () {
        return numUpdateVoltages;
    }

    public String toString () {
        return "<" + this.getClass().getName() + ", C=" + capacitance + "F>";
    }

    /**
     * Does nothing.
     */
    public void write (int address, int value, boolean word, long cycles) {
    }

    /**
     * @param address The address to read, must equal this.address for sanity
     * @param word Whether to interpret the result as a word; must be true for
     *             sanity
     * @param cycles Whatever
     * @return the <code>Capacitor</code>'s voltage as a fraction of its
     *         maximum voltage, multiplied by 65536 (the maximum value of an int
     *         on MSP430).  For example, if the cap's voltage is 5.0 V and its
     *         maximum voltage is 10.0, this method will return (5.0/10.0)
     *         65536 = 32768.  The consumer of this value should interpret this
     *         as saying that the voltage is at half its maximum level.
     */
    public int read (int address, boolean word, long cycles) {
//        System.err.println("Trapped read to voltage check");
        if ((address != voltageReaderAddress) || !word) {
            return 0;
        }

        double vfrac = voltage / effectiveMaxVoltage;
        int scaled_amt = (int)(Math.round(vfrac * 65536));

        setPowerMode(POWERMODE_ADC);
        cpu.cycles += ADC_CYCLES;
        updateVoltage(true /* assume we're in the checkpoint routine */);
        setPowerMode(POWERMODE_ACTIVE);
        return scaled_amt;
    }

    /**
     * Sets A, the initial condition.
     * @param newValue The new value for A
     */
    public void setA (double newValue, boolean dead) {
        A = newValue;
        lastATime = clockSource.getTimeMillis();
        if (!dead)
            lastATime += cpu.getOffset();
    }

    /**
     * Returns the load resistance to be used in voltage calculations.
     */
    public double getResistance () {
    	double resistance;
    	
        switch (powerMode) {
        	case POWERMODE_OFF: resistance = resistanceOff(voltage); break;
            case POWERMODE_ACTIVE: resistance = resistanceActive(voltage); break;
            case POWERMODE_LPM0:   resistance = resistanceLPM0(voltage); break;
            case POWERMODE_LPM1:   resistance = resistanceLPM1(voltage); break;
            case POWERMODE_LPM2:   resistance = resistanceLPM2(voltage); break;
            case POWERMODE_LPM3:   resistance = resistanceLPM3(voltage); break;
            case POWERMODE_LPM4:   resistance = resistanceLPM4(voltage); break;
            case POWERMODE_FLWRI:  resistance = resistanceFlashWrite(voltage); break;
            case POWERMODE_ADC:    resistance = resistanceADCRead(voltage); break;
            default: 
            	throw new RuntimeException("Unknown power mode " + powerMode);
        }
        return resistance + changeResistance();
    }
    
    public double getCurrent () {
    	return voltage / getResistance();
    }
    
    public double changeResistance ()
    {
    	double resistancesum = 0.0;
    	double resistancemul = 1.0;
    	
    	for (ResistanceState state : peripheralMap.values()) {
           	
    		if (state.getMode() == ResistanceState.ACTIVE)	// If active, then reduce resistance
           	{
    			double single_resistance = voltage / state.getCurrent();
           		resistancesum += single_resistance; 
           		resistancemul *= single_resistance;
           	} 
        }
    	
    	if (resistancesum == 0.0) {
    		return 0.0;
    	} else {
    		return resistancemul / resistancesum;
    	}
    }

    /* Sets the power mode (e.g., active, LPM0, ...).  The constants are defined
     * in se.sics.mspsim.core.MSP430Constants.MODE_NAMES. */
    public void setPowerMode (int mode) {
        //if (DEBUG) {
        	System.err.print("Capacitor.setPowerMode ");
        
        	// switch (powerMode) { This should be switch(mode) Matt's note
        	switch (mode) {
        		case POWERMODE_OFF:  System.err.println("OFF"); break;
        		case POWERMODE_ACTIVE: System.err.println("ACTIVE"); break;
        		case POWERMODE_LPM0: System.err.println("LPM0"); break;
        		case POWERMODE_LPM1: System.err.println("LPM1"); break;
        		case POWERMODE_LPM2: System.err.println("LPM2"); break;
        		case POWERMODE_LPM3: System.err.println("LPM3"); break;
        		case POWERMODE_LPM4: System.err.println("LPM4"); break;
        		case POWERMODE_FLWRI: System.err.println("FLWRI"); break;
        		case POWERMODE_ADC: System.err.println("ADC"); break;
        	}	
       // }
        
        // If the state went from active -> any LPM mode, FRAM always enter inactive state
        if (powerMode == POWERMODE_ACTIVE && (mode == POWERMODE_LPM0 || mode == POWERMODE_LPM1 
        							|| mode == POWERMODE_LPM2 || mode == POWERMODE_LPM3 || mode == POWERMODE_LPM4))
        {
        	FramController.setPowerState(false);
        	// System.out.println("Turning off FRAM power...");
        } else if ((powerMode == POWERMODE_LPM0 || powerMode == POWERMODE_LPM1  || powerMode == POWERMODE_LPM2 
        				|| powerMode == POWERMODE_LPM3 || powerMode == POWERMODE_LPM4) && mode == POWERMODE_ACTIVE)
        {
        	// In the case the state went from any LPM mode -> Active, FRAM always enters the state determined by the FRPWR bit
        	FramController.setPowerStateByPWRBit(); 
        	// System.out.println("Turning FRAM power to it's state determined by the FRPWR bit which is... " + FramController.getPowerState());
        }

        
        this.powerMode = mode;
    }

    public void toggleHush () {
        suppressVoltagePrinting = !suppressVoltagePrinting;
        suppressEnergyPrinting = !suppressEnergyPrinting;
    }
    
    public boolean updateVoltage() throws IVSurfaceEndedException {
        if (!enabled) return false;    		
		
        // Integrate the current into the RC, and set the R when in POWERMODE_OFF using the IV curve
        double current = ekhoFairyPrevCurrent;
        double C = capacitance;
        double Vc = voltage;
        boolean dead = (clockSource instanceof DeadTimer);
        double currentTime = clockSource.getTimeMillis();
        if (!dead)
            currentTime += cpu.getOffset();
        double dt = currentTime - lastATime;
        double currentTimeSeconds = currentTime / 1000.0;
        boolean shouldDie = false;
        
        if(ekhoFairy != null) {
        	// Check if the IV surface is done, if so, then END
        	if(currentTime / 1000.0 > maxEkhoRuntime) throw new IVSurfaceEndedException("IV Surface named \""+ekhoFairy.getName()+"\" ended ");
        	double Vh = ekhoFairy.getVoltage(currentTimeSeconds, current); // current time to seconds
        	double Rh = Vh / current; 
        	
        	// If we are charging, take into the account the load
        	if(Vh > ekhoFairyPrevVoltage) {
        		double Q = C * Vh * (1 - Math.exp( (-1.0 * currentTimeSeconds) / (R_ESR * C)) );
        		Vc = Q / C;
        		//Vc = Vh * (1 - Math.exp((-dt) / (Rh * C) ) );
        		Vc = Math.min(Vc, ekhoFairy.getMaximumVoltage());
        		setVoltage(Vc);
        	} else {
        		// Else discharge
        		double RC = getResistance() * capacitance;
                setVoltage(
                        voltage * // initial condition
                        Math.exp(
                                (-1.0 * currentTimeSeconds) // time
                                / (800.0 * RC)
                        )
                );
        	}
        	double I_load = voltage / getResistance();
        	double I_cap = (Vh / R_ESR) * Math.exp((-1.0 * currentTimeSeconds) / (R_ESR * C) );
        	//double I_cap = ( I_load * getResistance() - Vc ) / R_ESR;
        	 
            ekhoFairyPrevVoltage = voltage;
            ekhoFairyPrevCurrent = I_cap;
            System.out.println(voltage+", "+I_cap);
        }
        
        this.setA(getVoltage(), dead);
        
        // Update energy logs
        if(ekhoFairy != null) ekhologger.logEnergy(currentTime, current, voltage, ekhoFairyPrevVoltage);
        
        if (dead && !suppressVoltagePrinting) {
            if (printCounter++ % 10 == 0)
                System.err.format("<dead>%1.3f,%1.3f%n", currentTime, getVoltage());
            return false;
        }

        if (voltage <= cpu.deathThreshold) {
            shouldDie = true;
        }
        return shouldDie;
    }

    /**
     * P = E/t = VI, so E=tVI.
     * @return true if it's time to die, false otherwise
     */
    public boolean updateVoltage (boolean inCheckpoint) throws IVSurfaceEndedException {
        if (!enabled) return false;    		
		
        // Integrate the current into the RC, and set the R when in POWERMODE_OFF using the IV curve
        double RC = getResistance() * capacitance;
        double current = getCurrent();
        
        boolean dead = (clockSource instanceof DeadTimer);
        double currentTime = clockSource.getTimeMillis();
        if (!dead)
            currentTime += cpu.getOffset();
        double dt = currentTime - lastATime;
        boolean shouldSetVoltage = true;
        boolean shouldDie = false;
        
        // Give the fairy a crack at the voltage first.
        if(ekhoFairy != null) {
        	// Check if the IV surface is done, if so, then END
        	if(currentTime / 1000.0 > maxEkhoRuntime) throw new IVSurfaceEndedException("IV Surface named \""+ekhoFairy.getName()+"\" ended ");
        	//double V_applied = ekhoFairyPrevVoltage + ((ekhoFairyPrevCurrent + current) / (2*capacitance)) * dt;
            double V_applied = ekhoFairy.getVoltage(currentTime / 1000.0, current); // current time to seconds
             
            // If charging the capacitor (take into account load
            if (V_applied > ekhoFairyPrevVoltage) { // eFairy wants to add energy
            	//double Ic = ( Il * Rl - Vc ) / Resr;
                double V_initial = voltage; // V = Q/C
                double V_final = V_initial +
                    V_applied * (1 - Math.exp((-dt) / 800.0*RC));
                
                V_final = Math.min(V_final, ekhoFairy.getMaximumVoltage());
                setVoltage(V_final);
                // setVoltage(V_applied); // possibly large jump!

                double deltaV = V_final - V_initial;
                // System.err.println("Added: " + deltaV + "V");
                shouldSetVoltage = false;
            } else {
            	// If discharging
            	shouldSetVoltage = true;
            }
            ekhoFairyPrevVoltage = V_applied;
            ekhoFairyPrevCurrent = current;
        } else if (eFairy != null) {
        	double V_applied = eFairy.getVoltage(currentTime);
            if (V_applied > eFairyPrevVoltage) { // eFairy wants to add energy
                double V_initial = voltage; // V = Q/C
                double V_final = V_initial +
                    V_applied * (1 - Math.exp((-dt) / 800.0*RC));
                setVoltage(V_final);
                // setVoltage(V_applied); // possibly large jump!

                double deltaV = V_final - V_initial;
                System.err.println("Added: " + deltaV + "V");
                shouldSetVoltage = false;
            }
            eFairyPrevVoltage = V_applied;
        }
        if (shouldSetVoltage)
            setVoltage(
                    voltage * // initial condition
                    Math.exp(
                            (-1.0 * dt) // time
                            / (800.0 * RC)
                    )
            );
        
        System.out.println(voltage);
        this.setA(getVoltage(), dead);
        System.out.println(voltage+", "+current);
        // Update energy logs
        if(ekhoFairy != null) ekhologger.logEnergy(currentTime, current, voltage, ekhoFairyPrevVoltage);
        
        if (dead && !suppressVoltagePrinting) {
            if (printCounter++ % 10 == 0)
                System.err.format("<dead>%1.3f,%1.3f%n", currentTime, getVoltage());
            return false;
        }

        if (voltage <= cpu.deathThreshold) {
            // accumCycleCount += cpu.cycles;
            shouldDie = true;
        }

        if (!suppressVoltagePrinting && (printCounter++ % 10 == 0))
            System.err.format("%s%1.3f,%1.3f%n", (inCheckpoint ? "<chk>" : ""),
                    (clockSource.getTimeMillis() + cpu.getOffset()),
                    voltage);

        return shouldDie;
    }

    public void setClockSource (CapClockSource c) {
        this.clockSource = c;
    }

    /**
     *
     * @return the energy in Joules
     */
    public double getEnergy () {
        return (0.5 * capacitance * voltage * voltage);
    }

    public double getEffectiveMaxVoltage () {
        return effectiveMaxVoltage;
    }

    public long getNumLifecycles () {
        return numLifecycles;
    }

    /* @param cyclesToAdd add this many cycles to the accumulated count too */
    public void incrementNumLifecycles (long cyclesToAdd) {
        accumCycleCount += cyclesToAdd;
        ++numLifecycles;
    }

    public String getName () {
        return "Capacitor";
    }

    public void interruptServiced (int vector) {
    }

    public int getPowerMode() {
        return powerMode;
    }

    public void disable () {
        suppressVoltagePrinting = true;
        suppressEnergyPrinting = true;
        enabled = false;
    }

    public boolean isEnabled () {
        return enabled;
    }
    
    public void setPeripheralLow (Peripheral peripheral)
    {
    	ResistanceState state = peripheralMap.get(peripheral); 
    	if (state == null) // This should never, ever be null
    	{
    		throw new RuntimeException("Tried to set peripheral low without adding it to the Capicator's peripheral map first!"); 
    	}
    	
    	if (state.getMode() == ResistanceState.ACTIVE)	// If previous state was active, then the state has now changed
    		state.setChangedState(true);
    	
    	state.setMode(ResistanceState.INACTIVE);
    	
    	// System.err.println("Set chip low");
    }
    
    public void setPeripheralHigh (Peripheral peripheral)
    {
    	ResistanceState state = peripheralMap.get(peripheral); 
    	if (state == null) // This should never, ever be null
    	{
    		throw new RuntimeException("Tried to set peripheral high without adding it to the Capicator's peripheral map first!");
    	}
    	
    	if (state.getMode() == ResistanceState.INACTIVE)	// If previous state was inactive, then the state has now changed
    		state.setChangedState(true);
    	
    	state.setMode(ResistanceState.ACTIVE);
    	
    	// System.err.println("Set chip high");
    }
    
    public void addPeripheral (Peripheral peripheral, double current)
    {
    	ResistanceState state = new ResistanceState(false, current);
    	peripheralMap.put(peripheral, state);
    }
    
    public void changePeripheralCurrent (Peripheral peripheral, double current)
    {
    	ResistanceState state = peripheralMap.get(peripheral);
    	state.setCurrent(current);
    	// System.out.println("Changing peripheral current to " + current);
    }
    
    private double getCurrentTime () {
    	double currentTime = clockSource.getTimeMillis();
    	boolean dead = (clockSource instanceof DeadTimer);
    	if (!dead)
            currentTime += cpu.getOffset();
    	
    	return currentTime;
    }

    
	/**
	 * Used for Semiadaptive
	 * Returns 1 if the voltage is over 3.2, 0 otherwise.
	 */
	@Override
	public int nextData(int adcPos) {
		int retVal = (voltage > 3.2) ? 1 : 0;
		System.out.println("\nreal V:  " + voltage);
		return retVal;
	}
	
	
	/**
	 * Used for fully adaptive
	 * Returns the voltage, transformed to an integer in the range of signed bytes
	 * I think that is right for the ADC.
	 * It also assumes the voltage will be between 2 and 6.
	 * 
	 * Voltage = 4 + (retVal / 32)
	 */
	/*@Override
	public int nextData(int adcPos) {
		int retVal = (int) (32 * (voltage - 4));
		System.out.println("\nreal V:  " + voltage);
		return retVal;
	}*/

	@Override
	public void portWrite(IOPort source, int data) {
		// TODO Auto-generated method stub
		
	}
}
