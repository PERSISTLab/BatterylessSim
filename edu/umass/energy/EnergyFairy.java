package edu.umass.energy;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Scanner;

import se.sics.mspsim.util.Tuple;

public class EnergyFairy {

    private ArrayList<Tuple> traceValues;
    private ListIterator<Tuple> iter;
    private Tuple current;
    private Tuple next;
    private double maxTime;
    private double prevTime;

    public EnergyFairy(String tracePath) {
        this.traceValues = new ArrayList<Tuple>();
        String [] tupleValues = new String[2];

        boolean firstLine = true;
        double offset = 0;
        Scanner lineScanner = null;
        try {
            lineScanner = new Scanner(new File(tracePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        lineScanner.useDelimiter(System.getProperty("line.separator"));
        while (lineScanner.hasNext()) {
            tupleValues = lineScanner.next().split("\t");
            if(firstLine) {
                offset = Double.parseDouble(tupleValues[0]);
                firstLine = false;
            }
            //For timestamp, convert to double, subtract offset, put in list.
            //For voltage, just put it in the list
            traceValues.add(new Tuple((Double.parseDouble(tupleValues[0])-offset), (Double.parseDouble(tupleValues[1]))));
        }
        iter = traceValues.listIterator();
        current = iter.next();
        next = iter.next();

        //Find the maximum timestamp in the trace for wrapping purposes
        this.maxTime = traceValues.get(traceValues.size()-1).getX();
    }

    public double getVoltage(double time) {
        time %= maxTime;

        if (time < prevTime) { // handle time reset (happens in oracle mode)
            /* fast-forward to the current time */
            iter = traceValues.listIterator();
            current = iter.next();
            while (current.getX() < time)
                current = iter.next();
            current = iter.previous(); // and back up one
        }

        prevTime = time;

        if(time >= current.getX() && time < next.getX()) {
            return current.getY();
        }

        while(iter.hasNext()) {
            current = next;
            next = iter.next();
            if(time >= current.getX() && time < next.getX()) {
                return current.getY();
            }
        }
        iter = traceValues.listIterator();
        return getVoltage(time);
    }

    public double getMaxTime() {
        return maxTime;
    }

}
