Make sure to set MSPGCC_ROOT to your HOME/ti/gcc/bin directory inside your bashrc or zshrc file.

You can run this in eclipse with the following program arguments:

	-nogui 
	-exitwhendone 
	-platform=mayfly	
	-voltagetrace=traces/1.txt 
	-autorun=scripts/simple.sc 
	$(PATH_TO_REPO)/mspsim-ekho/firmware/mayfly/$(FIRMWARE_FOLDER/)main.out
	
