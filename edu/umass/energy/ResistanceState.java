package edu.umass.energy;

public class ResistanceState {

	private double current;
	private boolean mode;
	private boolean changedState;

	public static final boolean ACTIVE = true;
	public static final boolean INACTIVE = false;
	
	public ResistanceState (boolean mode, double current) {
		this.current = current;
		this.mode = mode;
		changedState = false;
	}
	
	public double getCurrent() {
		return current;
	}
	
	public void setCurrent(double current) {
		this.current = current;
	}
	
	public boolean getMode() {
		return mode;
	}
	
	public void setMode(boolean mode) {
		this.mode = mode;
	}
	

	public boolean isChangedState() {
		return changedState;
	}

	public void setChangedState(boolean switchedState) {
		this.changedState = switchedState;
	}
}
