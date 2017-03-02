package edu.umass.energy;

public class DeadTimer implements CapClockSource {

    double currentTime = 0;

    public DeadTimer(double initValue) {
        currentTime = initValue;
    }

    public double getTimeMillis() {
        return currentTime+=1;
    }

    public void setCurrentTime(double curTime) {
        this.currentTime = curTime;
    }

    public void reset() {
        setCurrentTime(0);
    }

    public double getOffset() {
        System.err.println("No");
        System.exit(-1);
        return 0;
    }

}
