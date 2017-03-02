package se.sics.mspsim.util;

import java.util.LinkedList;
import java.util.List;

/*
 * Singleton class
 */
public class QuickSim {

	private static List<String> traces;
	private static QuickSim quicksim;
	private static int counter = 0; 
	private static int max_runs = 1;
	
	private QuickSim() {
		traces = new LinkedList<>();
	}
	
	public static QuickSim getInstance () {
		if (quicksim == null) {
			quicksim = new QuickSim();
		}
		return quicksim;
	}
	
	public void addTrace (String trace) {
		traces.add(trace);
	}
	
	public String getTrace () {
		return traces.get(counter++);
	}
	
	public List<String> getTraces () {
		return traces;
	}
	
	public int traceLength () {
		return traces.size() - 1;
	}
	
	public int getMaxRuns () {
		return max_runs;
	}
}
