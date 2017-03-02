package se.sics.mspsim.chip;

import se.sics.mspsim.chip.CC2420.Reg;
import se.sics.mspsim.chip.CC2420.SpiState;
import se.sics.mspsim.core.EmulationLogger.WarningType;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOPort.PinState;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.TimeEvent;
import se.sics.mspsim.core.USARTListener;
import se.sics.mspsim.core.USARTSource;
import se.sics.mspsim.util.ArrayFIFO;
import se.sics.mspsim.util.Utils;
import edu.umass.energy.Capacitor;

public class CC1101 extends Radio802154 implements USARTListener, SPIData {

	// Energy variables (assume frequency band is 433 MHz for all)
	private Capacitor c;
	private double lastReadTime = 0.0;
	private static final int RADIO_POWER_BIT = 0x01; // Bit 1
	
	// TODO JOSIAH: Matt has no idea what this should be... Should be 1/datarate
	private static double symbol_period = 0.016; // 16 us
	
	// Chip status bytes
	private static final int STATUS_IDLE = 0x00;
	private static final int STATUS_RX_ACTIVE = 0x01;
	private static final int STATUS_TX_ACTIVE = 0x02;
	
	// Configuration register definitions 
	private static final int IOCFG2 = 0x00;
	private static final int IOCFG1 = 0x01;
	private static final int IOCFG0 = 0x02;
	private static final int FIFOTHR = 0x03;
	private static final int SYNC1 = 0x04;
	private static final int SYNC0 = 0x05;
	private static final int PKTLEN = 0x06;
	private static final int PKTCTRL1 = 0x07;
	private static final int PKTCTRL0 = 0x08;
	private static final int ADDR = 0x09;
	private static final int CHANNR = 0x0a;
	private static final int FSCTRL1 = 0x0b;
	private static final int FSCTRL0 = 0xc;
	private static final int FREQ2 = 0x0d;
	private static final int FREQ1 = 0x0e;
	private static final int FREQ0 = 0x0f;
	private static final int MDMCFG4 = 0x10;
	private static final int MDMCFG3 = 0x11;
	private static final int MDMCFG2 = 0x12;
	private static final int MDMCFG1 = 0x13;
	private static final int MDMCFG0 = 0x14;
	private static final int DEVIATN = 0x15;
	private static final int MCSM2 = 0x16;
	private static final int MCSM1 = 0x17;
	private static final int MCSM0 = 0x18;
	private static final int FOCCFG = 0x19;
	private static final int BSCFG = 0x1a;
	private static final int AGCCTRL2 = 0x1b;
	private static final int AGCCTRL1 = 0x1c;
	private static final int AGCCTRL0 = 0x1d;
	private static final int WOREVT1 = 0x1e;
	private static final int WOREVT0 = 0x1f;
	private static final int WORCTRL = 0x20;
	private static final int FREND1 = 0x21;
	private static final int FREND0 = 0x22;
	private static final int FSCAL3 = 0x23;
	private static final int FSCAL2 = 0x24;
	private static final int FSCAL1= 0x25;
	private static final int FSCAL0 = 0x26;
	private static final int RCCTRL1 = 0x27;
	private static final int RCCTRL0 = 0x28;
	private static final int FSTEST = 0x29;
	private static final int PTEST = 0x2a;
	private static final int AGCTEST = 0x2b;
	private static final int TEST2 = 0x2c;
	private static final int TEST1 = 0x2d;
	private static final int TEST0 = 0x2e;
	
	// Status registers
	private static final int PARTNUM = 0xf0;
	private static final int VERSION = 0xf1;
	private static final int FREQEST = 0xf2;
	private static final int LQI = 0xf3;
	private static final int RSSI = 0xf4;
	private static final int MARCSTATE = 0xf5;
	private static final int WORTIME1 = 0xf6;
	private static final int WORTIME0 = 0xf7;
	private static final int PKTSTATUS = 0xf8;
	private static final int VCO_VC_DAC = 0xf9;
	private static final int TXBYTES = 0xfa;
	private static final int RXBYTES = 0xfb;
	private static final int RCCTRCL1_STATUS = 0xfc;
	private static final int RCCTRCL0_STATUS = 0xfd;
	
	// Command Registers
	private static final int SRES = 0x30;
	private static final int SFSTXON = 0x31;
	private static final int SXOFF = 0x32;
	private static final int SCAL = 0x33;
	private static final int SRX = 0x34;
	private static final int STX = 0x35;
	private static final int SIDLE = 0x36;
	private static final int SWOR = 0x38;
	private static final int SPWD = 0x39;
	private static final int SFRX = 0x3a;
	private static final int SFTX = 0x3b;
	private static final int SWORRST = 0x3c;
	private static final int SNOP = 0x3d;
	
	private static final int PATABLE = 0x3e;
	private static final int RX_TX_BUFF = 0x3f;
	
	// The Operation modes of the CC1101
    public static final int MODE_TXRX_OFF = 0x00;
    public static final int MODE_RX_ON = 0x01;
    public static final int MODE_TXRX_ON = 0x02;
    public static final int MODE_POWER_OFF = 0x03;
    public static final int MODE_MAX = MODE_POWER_OFF;
    private static final String[] MODE_NAMES = new String[] {
        "off", "listen", "transmit", "power_off"
    };
    
	// State Machine - Datasheet Figure 25 page 50
    public enum RadioState {
        POWER_DOWN(-1),
        SLEEP(0),
        IDLE(1),
        XOFF(2),
        RX(13),
        FSTXON(18),
        TX(19);

        private final int state;
        RadioState(int stateNo) {
            state = stateNo;
        }

        public int getFSMState() {
            return state;
        }
    };
    
    // RAM Addresses
    public static final int RAM_TXFIFO	= 0x000;
    public static final int RAM_RXFIFO	= 0x080;
    
    /* IO Ports and their respective pins */
    private IOPort gdo0Port = null;
    private int gdo0Pin;
    
    private IOPort gdo2Port = null;
    private int gdo2pin;
    
    private IOPort sclkPort = null;
    private int sclkPin;
    /* End of IO Ports */
    
    private boolean chipSelect;
    private SpiState state = SpiState.WAITING;
    private int usartDataPos;
    private int usartDataAddress;
    private int usartDataValue;
    private int status = STATUS_IDLE;
    private RadioState stateMachine;
    private double datarate;
    private int outputpower;
    private int[] registers = new int[64];
    
    // when performing a write burst of registers this flag is set!
    public static final int FLAG_WRITE_BURST = 0x40;
    // when reading registers this flag is set!
    public static final int FLAG_READ = 0x80;
    // when performing a read burst of registers this flag is set!
    public static final int FLAG_READ_BURST = 0xc0;
    public static final int FLAG_RAM = 0x1000;
    // When accessing RAM the second byte of the address contains
    // a flag indicating read/write
    public static final int FLAG_RAM_READ = 0x2000;
    
    private static int writeCounter;
    private static boolean writeBurst = false;
    private int txCursor;
    private int txfifoPos;
    private boolean txfifoFlush;	// TXFIFO is automatically flushed on next write
    private static final int MAX_WRITE_COUNTER = 7;
    
    // TODO: Possible cause for concern below
    private final int[] memory = new int[0x400]; /* total memory */
    private final ArrayFIFO rxFIFO = new ArrayFIFO("RXFIFO", memory, 0x3f, 64); // 64 byte rx buffer
    private int[] patable = new int[8];
    
    private TimeEvent sendEvent = new TimeEvent(0, "CC2520 Send") {
    	public void execute(long t) {
    		txNext();
    	}
    };
	
	public CC1101(String id, String name, MSP430Core cpu) {
		super("CC1101", "Radio", cpu);
		
		c = cpu.getCapacitor();
		c.addPeripheral(this, 200e-9); // current draw will be sleep state (.2 microA)
		
		setState(RadioState.SLEEP);
		setModeNames(MODE_NAMES);
        setMode(MODE_POWER_OFF);
        rxFIFO.reset();
        
        resetWriteCounter();
        
        reset();
	}
	
	private void reset() {
		registers[IOCFG2] = 0x29;
		registers[IOCFG1] = 0x2e;
		registers[IOCFG0] = 0x3f;
		registers[FIFOTHR] = 0x07;
		registers[SYNC1] = 0xd3;
		registers[SYNC0] = 0x91;
		registers[PKTLEN] = 0xff;
		registers[PKTCTRL1] = 0x04;
		registers[PKTCTRL0] = 0x45;
		registers[WORCTRL] = 0xf8;
		registers[MDMCFG1] = 0x22;
		registers[MDMCFG2] = 0x02;
		registers[MDMCFG3] = 0x22;
		registers[MDMCFG4] = 0x8c;
		registers[FREND0] = 0x10;
		// Default PATABLE setting is 0xC6
		for (int i = 0; i < patable.length; i++) {
			patable[i] = 0xc6;
		}
		setDataRate();
		// setOutputPower();
	}

	@Override
	public int getSPIData(int offset) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSPIDataLen() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void outputSPI(int data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dataReceived(USARTSource source, int data) {
		int oldStatus = status;
		if (logLevel > INFO) {
			log("byte received: " + Utils.hex8(data) +
					" (" + ((data >= ' ' && data <= 'Z') ? (char) data : '.') + ')' +
					" CS: " + chipSelect + " SPI state: " + state + " StateMachine: " + stateMachine);
		}
		
		// Add reset condition
		if (data == SRES) {
			reset();
			source.byteReceived(oldStatus);
			return;
		}

		if (chipSelect) {
			// Chip is not selected
		} else if (stateMachine != RadioState.POWER_DOWN) {			
			switch(state) {
			case WAITING:
				if ((data & FLAG_READ) != 0) {
					state = SpiState.READ_REGISTER;
				} else {
					state = SpiState.WRITE_REGISTER;
				}
				if ((data & FLAG_WRITE_BURST) != 0) {
					writeBurst = true;
				} else {
					writeBurst = false;
				}
				if ((data & FLAG_RAM) != 0) {
					state = SpiState.RAM_ACCESS;
					usartDataAddress = data & 0x7f;
				} else {
					// The register address 
					usartDataAddress = data & 0x3f;

					if (usartDataAddress == RX_TX_BUFF) { // Possible place for error:...
						// check read/write???
						//          log("Reading RXFIFO!!!");
						if (state == SpiState.READ_REGISTER) {
							state = SpiState.READ_RXFIFO;
						} else {
							state = SpiState.WRITE_TXFIFO;
						}
					} 
				}
				if (data >= SRES && data <= SNOP) {
					strobe(data);
					state = SpiState.WAITING;
				}
				break;

			case WRITE_REGISTER:				
				source.byteReceived(registers[usartDataAddress] & 0xff);// >> 8);
				usartDataValue = data;// << 8;

				setReg(usartDataAddress, usartDataValue);

				if (writeCounter >= MAX_WRITE_COUNTER)
					resetWriteCounter();
				
				break;
			case READ_REGISTER:
				if (usartDataPos == 0) {
					source.byteReceived(registers[usartDataAddress] >> 8);
					usartDataPos = 1;
				} else {
					source.byteReceived(registers[usartDataAddress] & 0xff);
					if (logLevel > INFO) {
						log("read from " + Utils.hex8(usartDataAddress) + " = "
								+ registers[usartDataAddress]);
					}
					state = SpiState.WAITING;
				}
				return;
			case WRITE_TXFIFO:
				if(txfifoFlush) {
					txCursor = 0;
					txfifoFlush = false;
				}
				// System.out.println("Writing data: " + data + " to tx: " + txCursor);

				if(txCursor == 0) {
					if ((data & 0xff) > registers[PKTLEN]) {
						logger.logw(this, WarningType.EXECUTION, "CC2420: Warning - packet size too large: " + (data & 0xff));
					}
				} else if (txCursor > 127) {
					logger.logw(this, WarningType.EXECUTION, "CC2420: Warning - TX Cursor wrapped");
					txCursor = 0;
				}
				memory[RAM_TXFIFO + txCursor] = data & 0xff;
				txCursor++;
				if (sendEvents) {
					sendEvent("WRITE_TXFIFO", null);
				}
				break;
			}
			source.byteReceived(oldStatus);
		} else {
			/* No VREG but chip select */
			source.byteReceived(0);
			logw(WarningType.EXECUTION, "**** Warning - writing to CC2420 when VREG is off!!!");
		}
	}
	
	// Needs to get information about when it is possible to write
	// next data...
	private void strobe(int data) {
		// Resets, on/off of different things...
		if (logLevel > INFO) {
			log("Strobe on: " + Utils.hex8(data) + " => " + Reg.values()[data]);
		}

		if( (stateMachine == RadioState.POWER_DOWN)) {
			if (logLevel > INFO) log("Got command strobe: " + data + " in POWER_DOWN.  Ignoring.");
			return;
		}

		switch (data) {
		case SNOP:
			if (logLevel > INFO) log("SNOP => " + Utils.hex8(status) + " at " + cpu.cycles);
			break;
		case SIDLE:
			//System.out.println("SIDLE => " + Utils.hex8(status) + " at " + cpu.cycles);
			// Exit RX/TX
			setState(RadioState.IDLE);
			// Turn off frequency synthesizer
			// Exit Wake-On-Radio mode if applicable
			break;
		case SFRX:
			//System.out.println("SFRX => " + Utils.hex8(status) + " at " + cpu.cycles);
			flushRX();
			setState(RadioState.IDLE);
			break;
		case STX:
			//System.out.println("STX => " + Utils.hex8(status) + " at " + cpu.cycles);
			status |= STATUS_TX_ACTIVE;
			setState(RadioState.TX);
			setGD0Pin(PinState.HI);
			setMode(MODE_TXRX_ON);
			
			txfifoPos = 0;
			txNext();
			break;
		case SFTX:
			//System.out.println("SFTX => " + Utils.hex8(status) + " at " + cpu.cycles);
			flushTX();
			break;
		case SRX:
			//System.out.println("SRX => " + Utils.hex8(status) + " at " + cpu.cycles);
			status |= STATUS_RX_ACTIVE;
			setState(RadioState.RX);
			
			setMode(MODE_RX_ON);
			break;
		default:
			//if (logLevel > INFO) {
			System.out.println("Unknown strobe command: " + data);
			//}
			break;
		}
	}
	
	private void txNext() {
		if(txfifoPos <= memory[RAM_TXFIFO]) {
			/*int len = memory[RAM_TXFIFO] & 0xff;
	      if (txfifoPos == len - 1) {
	          txCrc.setCRC(0);
	          for (int i = 1; i < len - 1; i++) {
	            txCrc.addBitrev(memory[RAM_TXFIFO + i] & 0xff);
	          }
	          memory[RAM_TXFIFO + len - 1] = txCrc.getCRCHi();
	          memory[RAM_TXFIFO + len] = txCrc.getCRCLow();
	      }*/
			if (txfifoPos > registers[PKTLEN]) {
				logw(WarningType.EXECUTION, "**** Warning - packet size too large - repeating packet bytes txfifoPos: " + txfifoPos);
			}
			if (rfListener != null) {
				System.out.println("transmitting byte: " + Utils.hex8(memory[RAM_TXFIFO + (txfifoPos & 0x7f)] & 0xFF));
				rfListener.receivedByte((byte)(memory[RAM_TXFIFO + (txfifoPos & 0x7f)] & 0xFF));
			}
			txfifoPos++;
			// Two symbol periods to send a byte...
			cpu.scheduleTimeEventMillis(sendEvent, symbol_period * 2);
		} else {
			System.out.println("**** Completed Transmission.");
			status &= ~STATUS_TX_ACTIVE;
			setState(RadioState.IDLE); // TODO: Not sure about this... or the line below it...
			setGD0Pin(PinState.LOW);
			/* Back to RX ON */
			setMode(MODE_RX_ON);
			txfifoFlush = true;
		}
	}
	
	private void setState (RadioState state) {
		stateMachine = state;
		switch(state) {
			case POWER_DOWN:
				break;
			case IDLE:
				// Only voltage regulator to digital part and crystal oscillator running = 1.7 mA
				c.changePeripheralCurrent(this, 1.7e-3);
				break;
			case RX:
				// current = 15.7 mA at 38.4 kBaud (which is double our actual kBaud)
				c.changePeripheralCurrent(this, 15.7e-3);
				break;
			case TX:
				// current based on patable value
				setPatableCurrent();
				break;
			case FSTXON:
				// Only the frequency synthesizer is running (FSTXON state) = 8.4 mA
				c.changePeripheralCurrent(this, 8.4e-3);
				break;
			case SLEEP:
				// SLEEP state = 200 nA
				c.changePeripheralCurrent(this, 200e-9);
				break;
			case XOFF:
				// XOFF state = 165 microA
				c.changePeripheralCurrent(this, 165e-6);
				break;
		}
	}
	
	// TODO: update any pins here?
	private void flushTX() {
		txCursor = 0;
	}

	// TODO: Maybe should update some pins here too?
	private void flushRX() {
		if (logLevel > INFO) {
			log("Flushing RX len = " + rxFIFO.length());
		}
		rxFIFO.reset();
	}


	@Override
	public boolean isReadyToReceive() {
		// TODO Auto-generated method stub
		return false;
	}

	/* Receive a byte from the radio medium
	 * @see se.sics.mspsim.chip.RFListener#receivedByte(byte)
	 */
	@Override
	public void receivedByte(byte data) {
		setGD0Pin(PinState.HI);
		
		rxFIFO.write(data);
		
		setGD0Pin(PinState.LOW); // This is wrong...
	}

	@Override
	public int getActiveChannel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getActiveFrequency() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getOutputPower() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getOutputPowerMax() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getOutputPowerIndicator() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getOutputPowerIndicatorMax() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getRSSI() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setRSSI(int rssi) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getLQI() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setLQI(int lqi) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getConfiguration(int parameter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getModeMax() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private void setReg(int address, int data) {
		int oldValue = registers[address];
		registers[address] = data;
		
		switch(address) {
		case PATABLE:
			patable[writeCounter] = data;
			
			// If we are in writeBurst mode, then increment the writeCounter
			if (writeBurst) {
				writeCounter++;
			}
		break;
		// Either of these registers could potentially affect the data rate
		case MDMCFG3:
		case MDMCFG4:
			setDataRate();
		break;
		}
		
		configurationChanged(address, oldValue, data);
	}
	
	public void setChipSelect(boolean select) {
		chipSelect = select;
		if (chipSelect) {
			resetWriteCounter();
			state = SpiState.WAITING;
		}
		
		// System.out.println("setting chipSelect: " + (chipSelect ? "HIGH" : "LOW"));
	}
	
	public void setGDO0Port(IOPort port, int pin) {
		gdo0Port = port;
		gdo0Pin = pin;
	}
	
	private void setGD0Pin (PinState state) {
		gdo0Port.setPinState(gdo0Pin, state);
	}

	public void setGDO2Port(IOPort port, int pin) {
		gdo2Port = port;
		gdo2pin = pin;
	}

	public void setSCLKPort(IOPort port, int pin) {
		sclkPort = port;
		sclkPin = pin;
	}
	
	private void resetWriteCounter() {
		writeCounter = 0;
		writeBurst = false;
	}
	
	// Rdata = ((256 + DRATE_M) * (2 ^ DRATE_E)) / (2^28) * Fxosc 
	// After writing registers, data rate should be ~19.588kBaud for test app
	private void setDataRate() {
		// Fxosc = 26 MHz = 26000
		datarate = (((256 + registers[MDMCFG3]) * Math.pow(2, registers[MDMCFG4] & 0xf)) / Math.pow(2, 28)) * 26000;
		//System.out.println("Datarate is " + datarate);
		symbol_period = 1 / datarate;
		//System.out.println("Symbol period is " + SYMBOL_PERIOD);
	}
	
	private void setPatableCurrent() {
		int index = registers[FREND0] & 0x03; // first 3 bits
		int value = patable[index];
		// All values at 433 MHZ as usual
		switch (value) {
			case 0xc0:
				c.changePeripheralCurrent(this, 29.1e-3);
				break;
			case 0xc1:
				c.changePeripheralCurrent(this, 28.3e-3);
				break;
			case 0xc2:
				c.changePeripheralCurrent(this, 27.6e-3);
				break;
			case 0xc3:
				c.changePeripheralCurrent(this, 26.9e-3);
				break;
			case 0xc4:
				c.changePeripheralCurrent(this, 26.3e-3);
				break;
			case 0xc5:
				c.changePeripheralCurrent(this, 25.7e-3);
				break;
			case 0xc6:
				c.changePeripheralCurrent(this, 25.2e-3);
				break;
			case 0xc7:
				c.changePeripheralCurrent(this, 24.7e-3);
				break;
			case 0xc8:
				c.changePeripheralCurrent(this, 24.2e-3);
				break;
			case 0xc9:
				c.changePeripheralCurrent(this, 23.8e-3);
				break;
			case 0xca:
				c.changePeripheralCurrent(this, 23.4e-3);
				break;
			case 0x80:
				c.changePeripheralCurrent(this, 20.6e-3);
				break;
			case 0xcb:
				c.changePeripheralCurrent(this, 23.0e-3);
				break;
			case 0x81:
				c.changePeripheralCurrent(this, 20.3e-3);
				break;
			case 0xcc:
				c.changePeripheralCurrent(this, 22.6e-3);
				break;
			case 0x82:
				c.changePeripheralCurrent(this, 20.0e-3);
				break;
			case 0xcd:
				c.changePeripheralCurrent(this, 22.3e-3);
				break;
			case 0x83:
				c.changePeripheralCurrent(this, 19.7e-3);
				break;
			case 0x84:
				c.changePeripheralCurrent(this, 19.4e-3);
				break;
			case 0xce:
				c.changePeripheralCurrent(this, 21.6e-3);
				break;
			case 0x85:
				c.changePeripheralCurrent(this, 19.1e-3);
				break;
			case 0x86:
				c.changePeripheralCurrent(this, 18.8e-3);
				break;
			case 0x87:
				c.changePeripheralCurrent(this, 18.5e-3);
				break;
			case 0x88:
				c.changePeripheralCurrent(this, 18.2e-3);
				break;
			case 0x89:
				c.changePeripheralCurrent(this, 17.9e-3);
				break;
			case 0x8a:
				c.changePeripheralCurrent(this, 17.6e-3);
				break;
			case 0x8b:
				c.changePeripheralCurrent(this, 17.3e-3);
				break;
			case 0xcf:
				c.changePeripheralCurrent(this, 19.3e-3);
				break;
			case 0x8c:
				c.changePeripheralCurrent(this, 17.1e-3);
				break;
			case 0x8d:
				c.changePeripheralCurrent(this, 16.8e-3);
				break;
			case 0x8e:
				c.changePeripheralCurrent(this, 16.2e-3);
				break;
			case 0x50:
				c.changePeripheralCurrent(this, 16.0e-3);
				break;
			case 0x60:
				c.changePeripheralCurrent(this, 15.9e-3);
				break;
			case 0x51:
				c.changePeripheralCurrent(this, 15.7e-3);
				break;
			case 0x61:
				c.changePeripheralCurrent(this, 15.6e-3);
				break;
			case 0x40:
				c.changePeripheralCurrent(this, 15.4e-3);
				break;
			case 0x52:
				c.changePeripheralCurrent(this, 15.3e-3);
				break;
			case 0x3f:
				c.changePeripheralCurrent(this, 21.1e-3);
				break;
			case 0x62:
				c.changePeripheralCurrent(this, 15.3e-3);
				break;
			case 0x3e:
				c.changePeripheralCurrent(this, 20.5e-3);
				break;
			case 0x53:
				c.changePeripheralCurrent(this, 15.0e-3);
				break;
			case 0x3d:
				c.changePeripheralCurrent(this, 19.9e-3);
				break;
			case 0x63:
				c.changePeripheralCurrent(this, 15.0e-3);
				break;
			case 0x3c:
				c.changePeripheralCurrent(this, 19.3e-3);
				break;
			case 0x54:
				c.changePeripheralCurrent(this, 14.8e-3);
				break;
			case 0x64:
				c.changePeripheralCurrent(this, 14.7e-3);
				break;
			case 0x3b:
				c.changePeripheralCurrent(this, 18.7e-3);
				break;
			case 0x55:
				c.changePeripheralCurrent(this, 14.5e-3);
				break;
			case 0x65:
				c.changePeripheralCurrent(this, 14.5e-3);
				break;
			case 0x2f:
				c.changePeripheralCurrent(this, 17.9e-3);
				break;
			case 0x3a:
				c.changePeripheralCurrent(this, 18.1e-3);
				break;
			case 0x56:
				c.changePeripheralCurrent(this, 14.3e-3);
				break;
			case 0x2e:
				c.changePeripheralCurrent(this, 17.5e-3);
				break;
			case 0x66:
				c.changePeripheralCurrent(this, 14.2e-3);
				break;
			case 0x39:
				c.changePeripheralCurrent(this, 17.5e-3);
				break;
			case 0x57:
				c.changePeripheralCurrent(this, 14.1e-3);
				break;
			case 0x2d:
				c.changePeripheralCurrent(this, 17.1e-3);
				break;
			case 0x67:
				c.changePeripheralCurrent(this, 14.0e-3);
				break;	
			case 0x8f:
				c.changePeripheralCurrent(this, 14.4e-3);
				break;
			case 0x2c:
				c.changePeripheralCurrent(this, 16.7e-3);
				break;
			case 0x38:
				c.changePeripheralCurrent(this, 16.9e-3);
				break;
			case 0x68:
				c.changePeripheralCurrent(this, 13.8e-3);
				break;
			case 0x2b:
				c.changePeripheralCurrent(this, 16.3e-3);
				break;
			case 0x69:
				c.changePeripheralCurrent(this, 13.6e-3);
				break;
			case 0x37:
				c.changePeripheralCurrent(this, 16.2e-3);
				break;
			case 0x6a:
				c.changePeripheralCurrent(this, 13.5e-3);
				break;
			case 0x2a:
				c.changePeripheralCurrent(this, 15.9e-3);
				break;
			case 0x6b:
				c.changePeripheralCurrent(this, 13.3e-3);
				break;
			case 0x36:
				c.changePeripheralCurrent(this, 15.6e-3);
				break;
			case 0x29:
				c.changePeripheralCurrent(this, 15.5e-3);
				break;
			case 0x6c:
				c.changePeripheralCurrent(this, 13.2e-3);
				break;
			case 0x6d:
				c.changePeripheralCurrent(this, 13.0e-3);
				break;
			case 0x28:
				c.changePeripheralCurrent(this, 15.1e-3);
				break;
			case 0x35:
				c.changePeripheralCurrent(this, 15.0e-3);
				break;
			case 0x27:
				c.changePeripheralCurrent(this, 14.7e-3);
				break;
			case 0x6e:
				c.changePeripheralCurrent(this, 12.8e-3);
				break;
			case 0x26:
				c.changePeripheralCurrent(this, 14.3e-3);
				break;
			case 0x34:
				c.changePeripheralCurrent(this, 14.4e-3);
				break;
			case 0x25:
				c.changePeripheralCurrent(this, 13.9e-3);
				break;
			case 0x33:
				c.changePeripheralCurrent(this, 13.8e-3);
				break;
			case 0x24:
				c.changePeripheralCurrent(this, 13.5e-3);
				break;
			case 0x1f:
				c.changePeripheralCurrent(this, 13.3e-3);
				break;
			case 0x1e:
				c.changePeripheralCurrent(this, 13.2e-3);
				break;
			case 0x1d:
				c.changePeripheralCurrent(this, 13.1e-3);
				break;
			case 0x6f:
				c.changePeripheralCurrent(this, 12.4e-3);
				break;
			case 0x1c:
				c.changePeripheralCurrent(this, 12.9e-3);
				break;
			case 0x23:
				c.changePeripheralCurrent(this, 13.1e-3);
				break;
			case 0x32:
				c.changePeripheralCurrent(this, 13.1e-3);
				break;
			case 0x1b:
				c.changePeripheralCurrent(this, 12.8e-3);
				break;
			case 0x1a:
				c.changePeripheralCurrent(this, 12.7e-3);
				break;
			case 0x19:
				c.changePeripheralCurrent(this, 12.6e-3);
				break;
			case 0x18:
				c.changePeripheralCurrent(this, 12.5e-3);
				break;
			case 0x22:
				c.changePeripheralCurrent(this, 12.6e-3);
				break;
			case 0xf:
			case 0x17:
			case 0x0e:
				c.changePeripheralCurrent(this, 12.4e-3);
				break;
			case 0x0d:
			case 0x0c:
			case 0x16:
				c.changePeripheralCurrent(this, 12.3e-3);
				break;
			case 0x31:
				c.changePeripheralCurrent(this, 12.5e-3);
				break;
			case 0x0b:
			case 0x15:
			case 0x0a:
				c.changePeripheralCurrent(this, 12.2e-3);
				break;
			case 0x09:
			case 0x08:
			case 0x14:
				c.changePeripheralCurrent(this, 12.1e-3);
				break;
			case 0x21:
				c.changePeripheralCurrent(this, 12.2e-3);
				break;
			case 0x07:
			case 0x06:
			case 0x13:
				c.changePeripheralCurrent(this, 12.0e-3);
				break;
			case 0x05:
			case 0x04:
			case 0x12:
				c.changePeripheralCurrent(this, 11.9e-3);
				break;
			case 0x03:
			case 0x11:
				c.changePeripheralCurrent(this, 11.8e-3);
				break;
			case 0x02:
			case 0x01:
			case 0x10:
				c.changePeripheralCurrent(this, 11.7e-3);
				break;
			case 0x20:
				c.changePeripheralCurrent(this, 11.8e-3);
				break;
			case 0x30:
				c.changePeripheralCurrent(this, 11.9e-3);
				break;
			case 0x0:
				c.changePeripheralCurrent(this, 11.3e-3);
				break;
		}
	}
}
