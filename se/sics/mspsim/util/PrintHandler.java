package se.sics.mspsim.util;

import edu.clemson.eval.EvalLogger;

public class PrintHandler {

	private static final String GRAPH_EVENT = "GRAPH-EVENT";
	private static final String PRINT = "PRINTF";
	
	private EvalLogger evallogger;
	
	public PrintHandler() {}
	
	public PrintHandler(String name) {
		evallogger = EvalLogger.getInstance(name);
	}

	public void handleCommand (String fullcommand) {
		String [] command = fullcommand.split(":", 2);

		switch (command[0]) { // Switch on the command (what comes before the semi-colon) 
			case GRAPH_EVENT:
				try {
					if (command[1].contains("GRAPH")) {
						// Not sure why this happens right now, but for the time being we'll ignore it...
						System.out.println("command = " + fullcommand);
					} else {
						evallogger.addSensorEvent(command[1] + "\n");
					}
				} catch (NullPointerException e) {
					System.err.println("Graphing events only works with an ekhotracedir specified!");
				}
				break;
			case PRINT:
				System.out.println("printf: "+ command[1]);
				break;
			default:
				System.err.println("Command not recognized!");
				System.out.println(fullcommand);
		}
	}
}
