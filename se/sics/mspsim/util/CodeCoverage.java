package se.sics.mspsim.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import edu.clemson.eval.EvalLogger;

public class CodeCoverage {

	private int total_conditions;
	private static Map<String, CodeCoverage> codecoverage = new HashMap<>();
	private Set<Integer> conditions_seen;
	private String name;
	private static boolean onehook = true;
	private static final String COVERAGE = "codecoverage/";
	
	private CodeCoverage (String name) {
		conditions_seen = new TreeSet<>(); // So that it's ordered
		this.name = name;
		if (onehook)
			this.attachShutDownHook();
		onehook = false;
	}
	
	public static CodeCoverage getInstance (String name) {
		if (codecoverage.get(name) == null) {
			codecoverage.put(name, new CodeCoverage(name));
		}
		return codecoverage.get(name);
	}

	private void attachShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				for (Entry<String,CodeCoverage> entry : codecoverage.entrySet()) {	
					try {
						entry.getValue().getResults();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	public void setTotalConditions (int total) {
		total_conditions = total;
	}
	
	public void addCondition (int condition) {
		conditions_seen.add(condition);
	}
	
	public void getResults () throws IOException {
		makeDirectory(EvalLogger.DIR + COVERAGE + EvalLogger.subdir);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(EvalLogger.DIR + COVERAGE + EvalLogger.subdir + name.substring(name.lastIndexOf("/") + 1, name.length())));
		writer.write("\n-------------------------------------------------------------------------------------\n");
		writer.write("\n\tThe following conditions were not seen in the run for: " + name + "\n\n");
		int count = 0;
		for (int i = 0; i < total_conditions; i++) {
			if (conditions_seen.contains(i)) {
				count++;
			} else {
				writer.write("\t\tID = " + i + "\n");
			}
		}
		double percentage = round(((double) count / total_conditions) * 100, 1);
		writer.write("\nCode coverage is " + percentage + "\n");
		writer.write("-------------------------------------------------------------------------------------\n");
		writer.close();
	}
	
	private void makeDirectory (String folder1) {
		File codecoveragefile = new File(folder1);
		
		codecoveragefile.mkdirs();
	}
	
	private static double round (double value, int precision) {
	    int scale = (int) Math.pow(10, precision);
	    return (double) Math.round(value * scale) / scale;
	}
	
	public void clearConditions () {
		conditions_seen.clear();
	}
}
