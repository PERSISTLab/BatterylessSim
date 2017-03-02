package edu.clemson.eval;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvalLogger {

	private List<Double> currents;
	private List<Double> capvoltages;
	private List<Double> ekhovoltages;
	private double firstTimestamp;
	private static final int LOGGING_RATE = 100000;
	private BufferedWriter ekhowriter;
	private BufferedWriter eventwriter;
	public static String subdir = "test/";
	public static final String DIR = "evals/";
	private static final String EKHODIR = "energy/";
	private static final String EVENTDIR = "events/";
	private static Map<String, EvalLogger> evalloggers = new HashMap<>();
	private static final String ENERGY_FORMAT = "Start_Time_ms, End_Time_ms, Current_I, Cap_Voltage_V, Ekho_Voltage_V\n";
	private EvalLogger (String name) {
		currents = new ArrayList<>();
		capvoltages = new ArrayList<>();
		ekhovoltages = new ArrayList<>();
		setFilename(name);
	}
	
	public static EvalLogger getInstance (String name) {
		if (evalloggers.get(name) == null) {
			System.out.println("new eval logger with name = " + name);
			evalloggers.put(name, new EvalLogger(name));
		}
		return evalloggers.get(name);
	}
	
	public static void setSubdir (String dir) {
		subdir = dir + "/";
	}
	
	public void logEnergy(double timestamp, double current, double capVoltage, double ekhoVoltage) {
		if (currents.size() == 0) {
			setFirstTimestamp(timestamp);
		}
		
		currents.add(current);
		capvoltages.add(capVoltage);
		ekhovoltages.add(ekhoVoltage);
		if (currents.size() >= LOGGING_RATE) {
			try {
				ekhowriter.write(firstTimestamp + "," + 
						timestamp + "," + 
						average(currents) + "," + 
						average(capvoltages) + "," + 
						average(ekhovoltages) + "\n");
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void addSensorEvent (String event) {	
		try {
			eventwriter.write(event);
			eventwriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private double average (List<Double> list) {
		double sum = 0.0;
		double length = list.size();
		
		for (Double item : list) {
			sum += item;
		}
				
		list.clear();
		
		return sum / length;
	}
	
	public static Map<String, EvalLogger> getMap () {
		return evalloggers;
	}
	
	private void setFirstTimestamp (double timestamp) {
		firstTimestamp = timestamp;
	}
	
	public void setFilename (String filename) {	
		String ekhofolder = DIR + EKHODIR + subdir;
		String eventfolder = DIR + EVENTDIR + subdir;
		
		try {
			makeDirectories(ekhofolder, eventfolder);
			
			// Energy
			ekhowriter = new BufferedWriter(new FileWriter(ekhofolder + "ekho_" + filename.substring(filename.lastIndexOf("/") + 1, filename.length())));
			// Write format
			ekhowriter.write(ENERGY_FORMAT);
			
			// Events
			eventwriter = new BufferedWriter(new FileWriter(eventfolder + "event_" + filename.substring(filename.lastIndexOf("/") + 1, filename.length())));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void makeDirectories (String folder1, String folder2) {
		File ekhofile = new File(folder1);
		File eventfile = new File(folder2);
		
		ekhofile.mkdirs();
		eventfile.mkdirs();
	}
	
	public void closeWriters () {
		try {
			ekhowriter.close();
			eventwriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
