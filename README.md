# SIREN

This repo contains the source code for the PERSIST labs adaptation of MSPSIM; an instruction level simulator for MSP430 processors.

The goal of this project is to inform the simulation of modern, FRAM enabled MSP430s with voltage traces, IV surfaces (for energy harvesting), and with common hardware components like TARDIS / CusTARD and the CC1101.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisities

You need [TI GCC for MSP430s.](http://www.ti.com/tool/msp430-gcc-opensource)

#### For Linux:

	chmod +x msp430-gcc-full-linux-installer-4.0.1.0.run
	./msp430-gcc-full-linux-installer-4.0.1.0.run
	Step through installation, and choose the default path (/home/$USER/ti/gcc)


To use the firmware makefiles, you must also be sure to set MSPGCC_ROOT to your $HOME/ti/gcc/bin directory inside your bashrc or zshrc file.

#### For Linux:

	vim ~/.bashrc
	(Add new line to file) export MSPGCC_ROOT="$HOME/ti/gcc/bin"
	source ~/.bashrc


### Installing

A step by step series of examples that tell you have to get a development env running

#### For Eclipse:

*You should be able to import the project automatically using the eclipse Import GUI. If not, delete eclipse project files (.classpath, .project) and try the below.*


After moving the project into Eclipse, add the necessary libraries to the bulid path by doing the following:
	
	1) Right-click on the project in the left menu 
	2) Select "Build Path/Add Libraries..."
	3) Select "User Library"
	4) Choose "User Libraries..." in the top right
	5) Create a new user libary that includes the .jar files from the lib file in this project's directory.

To run the project in Eclipse, first build the firmware you want to simulate, for example, from the main directory using a terminal / concolse / cmd:

	cd firmware/moo
	make

This should execute without errors or warnings if toolchain is setup correctly.

Now setup the Run configuration in eclipse, perform the following:

	1) Go to "Run/Run Configurations" in the menu bar at the top
	2) Create a new run configuration by clicking the new button in the top left
	3) Input "mspsim-ekho" into the "Project:" space and "se.sics.mspsim.Main" into the "Main class:" space.
	4) Now click "Arguments" on the tabbed menu and input the following into the "Program Arguments:" space...
		-nogui -exitwhendone -platform=<platform name> <path to firmware file> -autorun="<path to script>"
	5) An example would be as follows:
		-nogui -exitwhendone -platform=moo firmware/moo/main.out -autorun="scripts/simple.sc"

Other platforms include `wisp, exp6989, senseandsend` the exp6989 platform includes support for the Memory Protection Unit (MPU).

To run a single firmware with multiple IV surfaces, add the arguement -ekhotracedir=<pathtoekhotraces> to the command line arguement.

To run mulitple firmwares with multiple IV surfaces, use the python script in the home directory, runtests.py, as so:
	
	python runtests.py --make -firmwaredir=<path to dir with ONLY multiple executables> -ekhotracedir=<pathtoekhotraces>

To graph events or printf debug, simply insert the following lines into the appropriate sections, as selected by the user, and include lib/printf.h in your compilation:

Printf:
	
	siren_command("PRINTF: <statement in normal printf formatting>\n");

Event graphing:

	siren_command("GRAPH-EVENT: <statement in normal printf formatting>\n");

IT IS VERY IMPORTANT TO ALWAYS HAVE '\n' AT THE END OF YOUR PRINT STATEMENT. IT WILL NOT WORK PROPERLY WITHOUT THIS.

If desired, you can add your own siren commands by updating the se/sics/mspsim/utils/PrintHandler.java to include more commands. Simply add a new case to the switch statement and implement the details of what to do when SIREN experiences this event (e.g log to file, print to stdout, print to stderr, etc). 

If graphing events, the output will be stored in the folder ${SIREN_HOME}/evals/test{i} where i is the particular test. To graph these events and there associated energy traces, run the following in 

	python ${SIREN_HOME}/evals/graph_eval.py

If you would like to create new IV surfaces, simply edit the script ${SIREN_HOME}/ekho/surface_maker.py to include the new IV curves. The script should be simple to update, needing only a tuple of tuples containing x and y points for the IV curve and the open voltage if listed. Both are found on the particular energy harvesters datasheet. One can also adjust the amount of time for the IV surface, but be warned very large values could run out of memory when graphing. To fix this, simply comment out the graphing aspect, and it will simply create huge traces without the large surface graphs.

Code coverage and Visual Profiler

To use the code coverage and visual profiler tools, you must first inject debug statements into your code using ekhoshim.py. To instrument your code, run the following commands:

	${SIREN_HOME}/firmware/ekhoshim.py -n <number of input files> (repeated n times(<input file> <outputfile>)) <name of record file>

Note, ekhoshim.py does not currently work with preprocess directives, external libraries, or C++. This is all future work, and we reailize it is limiting, but there are workarounds, such as replacing uints with ints and then back to uints before compiling, but after code injection. After compiling your outputfiles into an executable, you can run them in SIREN using the following command line arguement to use the visual profiler:
	
	-visualprofiler=<name of record file from ekhoshim.py>

Code coverage will be enabled by default after the code injection. To step through the visual profiler, don't enable start in your script (eg. simple.sc), and then use the console/commandline input. The command, "nextfunc", will progress from one function to the next, highlighting where you are in the programs flow. All code coverage statistics will be written to their appropriate files (named by their ekhotrace) in ${SIREN_HOME}/evals/codecoverage.

Ekhotrace formatting

In order to emulate IV surfaces, they must be in the following format:

	<n> # this determines the number of columns for currents, 2**n + 1 = number of columns
	<min_voltage, max_voltage> # this is the min and max voltae of the energy harvester

	<time>, <2**n + 1 current values seperated by commas> # These lines are repeated with increasing times for each IV curve to create the full surface


## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.


## Authors

* **Matthew Furlong, Josiah Hester** - SIREN

This is based off work from other labs, specifically, we are heavily indebted to the following:

* **Joakim Eriksson, Adam Dunkels, Niclas Finne, Fredrik Osterlind, Thiemo Voigt** - *Initial MSPSIM* - [MSPSIM](https://github.com/mspsim/mspsim)
* **Ben Ransford** - *Mementos MSPSIM* - [MSPSIM Mementos](https://github.com/ransford/mspsim/tree/mementos)

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

## License

This project is licensed under the BSD 3-clause "New" or "Revised" License.

## Acknowledgments

* A large thanks to Austin Anderson for his work on the script to shim C code.
