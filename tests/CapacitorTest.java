package tests;
import edu.umass.energy.Capacitor;
import se.sics.mspsim.config.MSP430f2132Config;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.util.ComponentRegistry;

public class CapacitorTest extends Capacitor {
    public CapacitorTest (MSP430 msp, double C, double initVoltage,
            double inputVoltageDividerFactor,
            double inputVoltageReferenceVoltage) {
        super(msp, C, initVoltage, inputVoltageDividerFactor,
                inputVoltageReferenceVoltage);
    }
    public void runTest () {
        double testV;
        for (int x = 0; x <= 32; ++x) {
            testV = 1.8 + x * 0.1;
            setVoltage(testV);
            int vConverted = read(voltageReaderAddress, true, 0);
            System.out.println(testV + " " + vConverted);
        }
    }
    public static void main (String[] args) {
        MSP430 msp = new MSP430(0, new ComponentRegistry(),
            new MSP430f2132Config());
        CapacitorTest cap = new CapacitorTest(msp, 10e-6, 4.5, 3.0, 2.5);
        cap.runTest();
    }
}
