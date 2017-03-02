/**
 * Copyright (c) 2007, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of MSPSim.
 * -----------------------------------------------------------------
 *
 * GenericNode
 *
 * Author  : Joakim Eriksson
 */

package se.sics.mspsim.platform;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Vector;

import javax.swing.JFrame;

import se.sics.mspsim.cli.CommandHandler;
import se.sics.mspsim.cli.DebugCommands;
import se.sics.mspsim.cli.FileCommands;
import se.sics.mspsim.cli.MiscCommands;
import se.sics.mspsim.cli.NetCommands;
import se.sics.mspsim.cli.ProfilerCommands;
import se.sics.mspsim.cli.StreamCommandHandler;
import se.sics.mspsim.cli.WindowCommands;
import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.EmulationException;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.MSP430Config;
import se.sics.mspsim.core.MSP430Constants;
import se.sics.mspsim.core.StopExecutionException;
import se.sics.mspsim.extutil.highlight.HighlightSourceViewer;
import se.sics.mspsim.ui.ConsoleUI;
import se.sics.mspsim.ui.ControlUI;
import se.sics.mspsim.ui.JFrameWindowManager;
import se.sics.mspsim.ui.StackUI;
import se.sics.mspsim.ui.WindowUtils;
import se.sics.mspsim.util.ArgumentManager;
import se.sics.mspsim.util.CheckpointValidator;
import se.sics.mspsim.util.CodeCoverage;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.ConfigManager;
import se.sics.mspsim.util.ELF;
import edu.clemson.eval.EvalLogger;
import se.sics.mspsim.util.IHexReader;
import se.sics.mspsim.util.MapTable;
import se.sics.mspsim.util.OperatingModeStatistics;
import se.sics.mspsim.util.PluginRepository;
import se.sics.mspsim.util.QuickSim;
import se.sics.mspsim.util.RecordReader;
import se.sics.mspsim.util.StatCommands;
import se.sics.mspsim.util.Utils;
import edu.clemson.ekho.Ekho;
import edu.umass.energy.Capacitor;
import edu.umass.energy.EnergyFairy;

public abstract class GenericNode extends Chip implements Runnable {
 	
  private static final String PROMPT = "MSPSim>";

  protected final MSP430 cpu;
  protected final ComponentRegistry registry;
  protected ConfigManager config;

  protected String firmwareFile = null;
  protected ELF elf;
  protected OperatingModeStatistics stats;
  protected CheckpointValidator checkpointing = new CheckpointValidator(this);

  protected int expectedExitCode = -1;
  protected Vector<MemoryContainer> memoryCaptures = new Vector<MemoryContainer>();
  private long prevWasted = 10000000000L;
  private double defaultAdjustmentMagnitude = 0.2; 
  private double prevAdjustmentMagnitude = defaultAdjustmentMagnitude;

  public GenericNode(String id, MSP430Config config) {
    super(id, new MSP430(0, new ComponentRegistry(), config));
    this.cpu = (MSP430)super.cpu;
    this.registry = cpu.getRegistry();
  }

  public ComponentRegistry getRegistry() {
    return registry;
  }

  public MSP430 getCPU() {
    return cpu;
  }

  public abstract void setupNode();

  public void setCommandHandler(CommandHandler handler) {
    registry.registerComponent("commandHandler", handler);
  }

  public void setupArgs(ArgumentManager config) throws IOException {
    String[] args = config.getArguments();
    if (args.length == 0) {
      System.err.println("Usage: " + getClass().getName() + " <firmware>");
      System.exit(1);
    }
    firmwareFile = args[0];
    if (!(new File(firmwareFile)).exists()) {
      System.err.println("Could not find the firmware file '" + firmwareFile + "'.");
      System.exit(1);
    }
    if (config.getProperty("nogui") == null) {
      config.setProperty("nogui", "false");
    }
    /* Ensure auto-run of a start script */
    if (config.getProperty("autorun") == null) {
      File fp = new File("scripts/autorun.sc");
      if (fp.exists()) {
        config.setProperty("autorun", "scripts/autorun.sc");
      } else {
        try {
          File dir = new File(GenericNode.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
          fp = new File(dir, "scripts/autorun.sc");
          if (fp.exists()) {
            config.setProperty("autorun", fp.getAbsolutePath());
          }
        } catch (URISyntaxException e) {
          // Failed to find auto run script
        }
      }
    }

    if (firmwareFile.endsWith("ihex")) {
      // IHEX Reading
      int[] memory = cpu.memory;
      IHexReader reader = new IHexReader();
      reader.readFile(memory, firmwareFile);
    } else {
      loadFirmware(firmwareFile);
    }
    config.setProperty("firmwareFile", firmwareFile);

    String mapFile = config.getProperty("map");
    if (mapFile != null) {
      MapTable map = new MapTable(mapFile);
      cpu.getDisAsm().setMap(map);
      cpu.setMap(map);
      registry.registerComponent("mapTable", map);
    }
    
    setup(config);


    if (!config.getPropertyAsBoolean("nogui", false)) {
      // Setup control and other UI components
      ControlUI control = new ControlUI();
      registry.registerComponent("controlgui", control);
      registry.registerComponent("stackchart", new StackUI(cpu));
      HighlightSourceViewer sourceViewer = new HighlightSourceViewer();
      // Add the firmware location to the search path
      File fp = new File(firmwareFile).getParentFile();
      if (fp != null) {
          try {
              // Get absolute path
              fp = fp.getCanonicalFile();
          } catch (Exception e) {
              // Ignore
          }
          sourceViewer.addSearchPath(fp);
      }
      control.setSourceViewer(sourceViewer);
    }

    String script = config.getProperty("autorun");
    if (script != null) {
      File fp = new File(script);
      if (fp.canRead()) {
        CommandHandler ch = registry.getComponent(CommandHandler.class, "commandHandler");
        script = script.replace('\\', '/');
        System.out.println("Autoloading script: " + script);
        config.setProperty("autoloadScript", script);
        if (ch != null) {
          ch.lineRead("source \"" + script + '"');
        }
			} else {
				System.err.println("Cannot read file "
						+ config.getProperty("autorun"));
				System.exit(1);
      }
    }

		String expectedExitCodeStr = config.getProperty("expectedexitcode");
		if (null != expectedExitCodeStr) {
			expectedExitCodeStr = expectedExitCodeStr.trim();
			try {
				if (expectedExitCodeStr.startsWith("0x")) {
					this.expectedExitCode = Integer.parseInt(
							expectedExitCodeStr.substring(2), 16);
				} else {
					this.expectedExitCode = Integer
							.parseInt(expectedExitCodeStr);
				}
			} catch (NumberFormatException nfe) {
				System.err.println("-expectedexitcode argument must resemble "
						+ "1234 or 0xabc");
				System.exit(1);
			}
		}

    if (args.length > 1) {
        // Run the following arguments as commands
        CommandHandler ch = registry.getComponent(CommandHandler.class, "commandHandler");
        if (ch != null) {
            for (int i = 1; i < args.length; i++) {
                System.out.println("calling '" + args[i] + "'");
                ch.lineRead(args[i]);
            }
        }
    }
    
    cpu.setEkhoFilename(config.getProperty("ekhotrace"));
    
    System.out.println("-----------------------------------------------");
    System.out.println("MSPSim " + MSP430Constants.VERSION + " starting firmware: " + firmwareFile);
    System.out.println("-----------------------------------------------");
    System.out.print(PROMPT);
    System.out.flush();
  }

  public void setup(ConfigManager config) {
    this.config = config;

    registry.registerComponent("cpu", cpu);
    registry.registerComponent("node", this);
    registry.registerComponent("config", config);
    registry.registerComponent("checkpointing", checkpointing);
    
    CommandHandler ch = registry.getComponent(CommandHandler.class, "commandHandler");

    if (ch == null) {
        if (config.getPropertyAsBoolean("jconsole", false)) {
            ConsoleUI console = new ConsoleUI();
            PrintStream consoleStream = new PrintStream(console.getOutputStream());
            ch = new CommandHandler(consoleStream, consoleStream);
            JFrame w = new JFrame("ConsoleUI");
            w.add(console);
            w.setBounds(20, 20, 520, 400);
            w.setLocationByPlatform(true);
            String key = "console";
            WindowUtils.restoreWindowBounds(key, w);
            WindowUtils.addSaveOnShutdown(key, w);
            w.setVisible(true);
            console.setCommandHandler(ch);
        } else {
            ch = new StreamCommandHandler(System.in, System.out, System.err, PROMPT);
        }
        registry.registerComponent("commandHandler", ch);
    }
    
    stats = new OperatingModeStatistics(cpu);
    
    registry.registerComponent("pluginRepository", new PluginRepository());
    registry.registerComponent("debugcmd", new DebugCommands());
    registry.registerComponent("misccmd", new MiscCommands());
    registry.registerComponent("filecmd", new FileCommands());
    registry.registerComponent("statcmd", new StatCommands(cpu, stats));
    registry.registerComponent("wincmd", new WindowCommands());
    registry.registerComponent("profilecmd", new ProfilerCommands());
    registry.registerComponent("netcmd", new NetCommands());
    registry.registerComponent("windowManager", new JFrameWindowManager());

    // Monitor execution
    cpu.setMonitorExec(true);
    
    setupNode();

    registry.start();

    // attach a voltage trace file if "-voltagetrace" is specified on
    // cmdline
    String voltageTraceFile = config.getProperty("voltagetrace");
    if (null != voltageTraceFile) {
    	Capacitor c = cpu.getCapacitor();
    	c.setEnergyFairy(new EnergyFairy(voltageTraceFile));
    	c.setInitialVoltage(0.0);
    }
    String ekhoTraceFile = config.getProperty("ekhotrace");
    if (null != ekhoTraceFile) {
    	addEkhoFairy(ekhoTraceFile);
    }
    String recordFile = config.getProperty("visualtrace");
    if (null != recordFile) {
    	RecordReader reader = new RecordReader(recordFile);
    	reader.dagToFrame();
    }
    String ekhoTraceDir = config.getProperty("ekhotracedir");
    if (null != ekhoTraceDir) {
    	cpu.setQuickSim(true);
    }
    
    cpu.reset();
  }
  
  private void addEkhoFairy (String trace) {
	  Capacitor c = cpu.getCapacitor();
	  c.setEkhoFairy(new Ekho(trace));
	  c.setInitialVoltage(0.0);
  }
 
  public void run() {
    if (!cpu.isRunning()) {
      try {
        cpu.cpuloop(); 
      } catch (StopExecutionException see) {
        cpu.stop();
        System.err.println("Execution terminated (" + see.getMessage()
            + ")");
        
        boolean ewd = config
          .getPropertyAsBoolean("exitwhendone", false);
        if (this.expectedExitCode != -1) { // user specified
          // -expectedexitcode
          if (this.expectedExitCode == see.getR15Val()) {
            if (ewd) {
              stopExecution();
            }
          } else { // uh-oh, bad exit code
            System.err.println("Exit code "
                + Utils.hex16(see.getR15Val())
                + " did not match expected ("
                + Utils.hex16(expectedExitCode) + ")");
            if (ewd) {
              stopExecution();
            }
          }
        } else {
          if (ewd) {
            stopExecution();
          }
        }
        this.stop();
      } catch (Exception e) {
        /* what should we do here */
        e.printStackTrace();
      }
    }
  }
  
  private void stopExecution () {
	  if (!cpu.isQuickSim()) {
    	System.exit(0);
      }
  }
  
  public void start() {
    if (!cpu.isRunning()) {
      Thread thread = new Thread(this);
      // Set this thread to normal priority in case the start method was called
      // from the higher priority AWT thread.
      thread.setPriority(Thread.NORM_PRIORITY);
      thread.start();
    }
  }
  
  public void stop() {
    cpu.stop();
  }

  public void step() throws EmulationException {
    step(1);
  }

  // A step that will break out of breakpoints!
  public void step(int nr) throws EmulationException {
    if (!cpu.isRunning()) {
      cpu.stepInstructions(nr);
    }
  }

  public ELF loadFirmware(URL url) throws IOException {
      return loadFirmware(url, cpu.memory);
  }

  @Deprecated public ELF loadFirmware(URL url, int[] memory) throws IOException {
    DataInputStream inputStream = new DataInputStream(url.openStream());
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    byte[] firmwareData = new byte[2048];
    int read;
    while ((read = inputStream.read(firmwareData)) != -1) {
      byteStream.write(firmwareData, 0, read);
    }
    inputStream.close();
    ELF elf = new ELF(byteStream.toByteArray());
    elf.readAll();
    return loadFirmware(elf, memory);
  }

  public ELF loadFirmware(String name) throws IOException {
      return loadFirmware(name, cpu.memory);
  }

  @Deprecated public ELF loadFirmware(String name, int[] memory) throws IOException {
    return loadFirmware(ELF.readELF(firmwareFile = name), memory);
  }

  public ELF loadFirmware(ELF elf) {
      return loadFirmware(elf, cpu.memory);
  }

  @Deprecated public ELF loadFirmware(ELF elf, int[] memory) {
    if (cpu.isRunning()) {
        stop();
    }
    this.elf = elf;
    elf.loadPrograms(memory);
    MapTable map = elf.getMap();
    cpu.getDisAsm().setMap(map);
    cpu.setMap(map);
    registry.registerComponent("elf", elf);
    registry.registerComponent("mapTable", map);
    return elf;
  }

  public int getConfiguration(int param) {
      return 0;
  }

	public void captureMemory() {
		memoryCaptures.add(new MemoryContainer(cpu.memory));
}
	
	public MemoryContainer getLastMemoryCapture() {
		return memoryCaptures.lastElement();
	}

	/* CPU calls this method to reports its own death */
	public void reportDeath() throws ShouldRetryLifecycleException {
		long cycles = cpu.cycles;
		long wasted = cpu.getWastedCycles();
		double ot = cpu.getOracleThreshold();
		double adjustmentDirection = 0.0;
		double adjustmentMagnitude = 0.0;

		if (DEBUG) {
			System.err.println("reportDeath(" + ot + "): " + cycles + " cycles; "
				+ wasted + " wasted; ");
		
			/* report on the contents of memory */
			CheckpointValidator cv =
				(CheckpointValidator)cpu.getRegistry().getComponent("checkpointing");
			
			int activeBundle = cv.findActiveBundlePointer(cpu.memory);
			System.err.println("Active bundle: " + Utils.hex16(activeBundle));
			System.err.println("Segment @" +
					Utils.hex16(CheckpointValidator.SEGMENT_A) + " is " +
					(CheckpointValidator.segmentIsEmpty(cpu.memory,
							CheckpointValidator.SEGMENT_A)
								? "empty" : "nonempty") + ", is " +
					(CheckpointValidator.segmentIsMarkedForErasure(cpu.memory,
							CheckpointValidator.SEGMENT_A)
								? "marked for erasure" : "not marked for erasure"));
			System.err.println("Segment @" +
					Utils.hex16(CheckpointValidator.SEGMENT_B) + " is " +
					(CheckpointValidator.segmentIsEmpty(cpu.memory,
							CheckpointValidator.SEGMENT_B)
								? "empty" : "nonempty") + ", is " +
					(CheckpointValidator.segmentIsMarkedForErasure(cpu.memory,
							CheckpointValidator.SEGMENT_B)
								? "marked for erasure" : "not marked for erasure"));
		}
		
		if (ot < 0) return;
		
		MemoryContainer lastCapture = memoryCaptures.lastElement();
		System.err.println("Most recent (of " + memoryCaptures.size()
				+ ") capture length: " + lastCapture.getMemory().length);
		//stop();

		if (wasted == cycles) {
			// wasted the whole lifecycle; could do better by raising the oracle
			// threshold.  go up some amount, then go down by half that amount, for
			// effectively binary search
			adjustmentDirection = 1;
			adjustmentMagnitude = prevAdjustmentMagnitude;
		} else if (wasted > 0) {
			// didn't waste the whole lifecycle but wasted something; therefore
			// we could do better by lowering the oracle threshold
			if (wasted > prevWasted) {
				adjustmentMagnitude = prevAdjustmentMagnitude;
				throw new ShouldRetryLifecycleException("Refusing to " +
						"increase waste; Bumping oracle threshold by " +
						adjustmentMagnitude + " V", adjustmentMagnitude);
			} else if (wasted == prevWasted) {
				System.err.println("Oracle commits to Vthresh=" +
						cpu.getOracleThreshold() + " V (bottomed out)");
				prevWasted = 10000000000L;
				prevAdjustmentMagnitude = defaultAdjustmentMagnitude;
				return;
			} else {
				adjustmentDirection = -1;
				adjustmentMagnitude = prevAdjustmentMagnitude / 2.0;
				if (adjustmentMagnitude <= cpu.oracleEpsilon) {
					System.err.println("Oracle commits to Vthresh=" +
							cpu.getOracleThreshold() + " V (not split hairs)");
					prevWasted = 10000000000L;
					prevAdjustmentMagnitude = defaultAdjustmentMagnitude;
					return;
				}
			}
		} else if (wasted == 0) {
			System.err.println("Oracle commits to Vthresh=" +
					cpu.getOracleThreshold() + " V (nailed it)");
			prevWasted = 10000000000L;
			prevAdjustmentMagnitude = defaultAdjustmentMagnitude;
			return;
		} else {
			throw new RuntimeException("wtf");
		}
		prevAdjustmentMagnitude = adjustmentMagnitude;
		adjustmentMagnitude = adjustmentDirection * adjustmentMagnitude;
		throw new ShouldRetryLifecycleException("Bumping oracle threshold by " +
				adjustmentMagnitude + " V",
				adjustmentMagnitude);
		// try { Thread.sleep(10000); } catch (InterruptedException ie) {}
	}

	public String getFirmwareFileName () {
		return firmwareFile;
	}
}
