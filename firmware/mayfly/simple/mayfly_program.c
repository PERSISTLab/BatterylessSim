#include "mayfly_program.h"

#define STATE_NODE_IDLE 0
#define STATE_DEPENDANCIES_SATISFIED 1
#define STATE_NODE_COMPLETE 2

#define NUM_NODES 2
#define TIMESTAMP_DIVIDER 10 // Deciseconds

#define MAGIC_NUMBER 0xdeedbeef
/*
digraph G {
	0 [label="XL", output="[int x, int y, int z]" generation="[]"];
	1 [label="sink", output="[]", shape="doublecircle"];

	0 -> 1[label="[]"];
}
*/

// Sinks are highest priority, so always make progress
const uint8_t priority_list[NUM_NODES] = { 1, 0 };
const uint8_t source_node = 0;
volatile uint8_t i, next;
volatile int8_t size_buf;

/** Each of the edges (possibly including initial edge) has their own data structure with timestamp */
typedef struct  {
	uint16_t max_samples;
	uint8_t sample_size;
	uint16_t data[3];
	uint32_t timestamp[1];
	uint8_t head;
	uint8_t tail;
	uint8_t samples_in_buf;
} __XL_to_sink_data_t;
 __XL_to_sink_data_t XL_to_sink_data __attribute__ ((section ( ".upper.rodata" ))) = { 1, 3, {0}, {0}, 0, 0, 0}; 

typedef struct {
	uint32_t magic_number;
	uint8_t current_node_id;
	uint8_t node_status[NUM_NODES];
} mayfly_state_t;
mayfly_state_t __mayfly_state __attribute__ ((section ( ".upper.rodata" ))) = {0, 1, {0}};




/** Node function definitions from user code + mayfly additions */
void XL() {
	// Read on three ADC channels for accel readings starting at 1
	ADC12CTL0 &= ~ADC12ENC;
	ADC12CTL0 = ADC12ON | ADC12SHT0_2 | ADC12MSC;        // Turn on ADC12, set sampling time
  	ADC12CTL1 = ADC12SHP | ADC12CONSEQ_1;     // Use sampling timer, seq conversions
  	ADC12MCTL0 = ADC12INCH_1;
  	ADC12MCTL1 = ADC12INCH_2;
  	ADC12MCTL2 = ADC12INCH_3 | ADC12EOS;
  	ADC12CTL0 |= ADC12ENC | ADC12SC;     // Enable conversions, Start conversion-software trigger 
  	while (!(ADC12IFGR0 & BIT0));
	XL_to_sink_data.data[0] = ADC12MEM0;
	while (!(ADC12IFGR0 & BIT1));
	XL_to_sink_data.data[1] = ADC12MEM1;
	while (!(ADC12IFGR0 & BIT2));
	XL_to_sink_data.data[2] = ADC12MEM2;	
	siren_command("PRINTF: %u,%u,%u\n", XL_to_sink_data.data[0], XL_to_sink_data.data[1], XL_to_sink_data.data[2]);
	XL_to_sink_data.samples_in_buf = 1;
	__delay_cycles(10000);
}

void sink() {
	__delay_cycles(10000);
	siren_command("GRAPH-EVENT: sink\n");
}

/** Each node has a function to evaluate the dependancies, gets all incoming edges */
/*	and if all are satisfied, then returns true, assumes AND of all edges dependancies */
uint8_t __XL_dependancies_satisfied() {
	return 1;
}

uint8_t __sink_dependancies_satisfied() {
	// Only dependancy is that edge has data
	if(XL_to_sink_data.samples_in_buf > 0) {
		return 1;
	}
	return 0;
}

uint8_t dependancies_satisfied(uint8_t node_id) {
	uint8_t retval = 0;
	switch(node_id) {
		case 0:
			retval = __XL_dependancies_satisfied();
			break;
		case 1:
			retval = __sink_dependancies_satisfied();
			break;
	}
	return retval;
}

void mayfly_init(uint32_t current_time) {
	// Rollback if magic number not correct
	if(__mayfly_state.magic_number != MAGIC_NUMBER) {
		// Started, but did not make it through a task, so rollback outgoing edges data
		XL_to_sink_data.samples_in_buf = 0;
	}
}

void mayfly_schedule(uint32_t current_time) {
	for(i=0;i<NUM_NODES;i++) {
      // If all dependancies for this node are satisfied, execute it and break
      if(dependancies_satisfied(priority_list[i])) {
        __mayfly_state.current_node_id = priority_list[i];
        //node_status[__mayfly_checkpoint.current_node_id] = STATE_DEPENDANCIES_SATISFIED;
        __mayfly_state.magic_number = 0;
        switch(__mayfly_state.current_node_id) {
          case 0:
            XL();
            // Data on previous edge is now CONSUMED
            
            // Task completed, checkpoint, and add timing information
            XL_to_sink_data.timestamp[0] = current_time;
            break;
          case 1:
            sink();
            // Data on previous edge is now CONSUMED
            //memset(XL_to_sink_data.data, 0, 3);
            XL_to_sink_data.samples_in_buf = 0;
            
            // Task completed, checkpoint, and add timing information
            break;
        }

		// Made it through task
        __mayfly_state.magic_number = MAGIC_NUMBER;
        //node_status[__mayfly_checkpoint.current_node_id] = STATE_NODE_COMPLETE;       
        break;
      }
      // Else continue around to find a node that is satisfied. Note that source nodes will not always have deps
      // satisfied, as they are bound by hardware requirements (accelerometer can only )
    }
}