SOURCES 	=  main.c ../../lib/printf.c
OBJECTS 	=  main.o ../../lib/printf.o
EXE 		=  main.out
CC          = msp430-elf-gcc
DEVICE  	= msp430fr6989
GDB     	= $(MSPGCC_ROOT)/msp430-elf-gdb

# GCC flags
CSTD_FLAGS 			= -std=gnu11 -funsigned-char
DEBUG_FLAGS 		= -g3 -ggdb -gdwarf-2
ERROR_FLAGS 		= -Wall -Wextra -Wshadow -fmax-errors=5
NO_ERROR_FLAGS 		= -Wno-unused-parameter -Wno-unknown-pragmas -Wno-unused-variable -Wno-type-limits -Wno-comment
LIB_INCLUDES 		= -I $(MSPGCC_ROOT)/../include/ -I../../lib/
MSP430_FLAGS 		= -mmcu=$(DEVICE) -mhwmult=none -D__$(DEVICE)__ -DDEPRECATED
REDUCE_SIZE_FLAGS	= -fdata-sections -ffunction-sections -finline-small-functions -O0

CFLAGS 				= $(CSTD_FLAGS) $(DEBUG_FLAGS) $(ERROR_FLAGS) $(NO_ERROR_FLAGS) $(LIB_INCLUDES) $(MSP430_FLAGS) $(REDUCE_SIZE_FLAGS)
	 
LFLAGS = -Wl,--gc-sections -Wl,--reduce-memory-overheads -Wl,--stats -Wl,--relax
LIBS = -L $(MSPGCC_ROOT)/../include/

all: compile

%.o: %.c $(DEPS)
	$(CC) $(CFLAGS) -c $< -o $@


# Change -o to $(DEVICE) when we need multiple devices
compile: $(OBJECTS) 
	$(CC) $(CFLAGS) $(OBJECTS) $(LFLAGS) -o $(EXE) $(LIBS) 

install: all
	mspdebug tilib "prog $(EXE)"

debug: all
	mspdebug tilib gdb &

gdb:
	msp430-elf-gdb -x ../tools/cmd.gdb

clean:
	rm -rf $(OBJECTS)
	rm -f $(EXE)
