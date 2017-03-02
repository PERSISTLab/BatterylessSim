#include "mayfly_program.h"

#define STATE_NODE_IDLE 0
#define STATE_DEPENDANCIES_SATISFIED 1
#define STATE_NODE_COMPLETE 2
#define NUM_NODES 2
#define TIMESTAMP_DIVIDER 10 // Deciseconds

/*
digraph G {
	0 [label="XL", output="[int x, int y, int z]" generation="[]"];
	1 [label="sink", output="[]", shape="doublecircle"];

	0 -> 1[label="[]"];
}
*/

// Sinks are highest priority
const uint8_t priority_list[NUM_NODES] = { 1, 0 };
const uint8_t source_node = 0;
uint8_t node_status[NUM_NODES] = {0};
uint8_t current_node_id;
uint8_t	current_node_ndx; // In priority list
volatile uint8_t i, next;
volatile int8_t size_buf;
uint32_t current_time;	// Load from FRAM, this is the time since the duty cycle started in ms
						// This is the number of deciseconds since the begining of time

/** Each of the edges (possibly including initial edge) has their own data structure with timestamp */
typedef struct __attribute__((__packed__)) {
	uint16_t max_samples;
	uint8_t sample_size;
	uint16_t data[3];
	uint32_t timestamp[1];
	uint8_t head;
	uint8_t tail;
	uint8_t samples_in_buf;
} __XL_to_sink_data_t;
 __XL_to_sink_data_t XL_to_sink_data = { 1, 3, {0}, {0}, 0, 0, 0};


/** Node function definitions from user code + mayfly additions */
void XL() {
	// Read on three ADC channels for accel readings starting at 1
	ADC12CTL0 &= ~ADC12ENC;
	ADC12CTL0 = ADC12ON | ADC12SHT0_2 | ADC12MSC;        // Turn on ADC12, set sampling time
  	ADC12CTL1 = ADC12SHP | ADC12CONSEQ_1;     // Use sampling timer, seq conversions
  	ADC12MCTL0 = ADC12INCH_1;
  	ADC12MCTL1 = ADC12INCH_2;
  	ADC12MCTL2 = ADC12INCH_3;
  	ADC12CTL0 |= ADC12ENC | ADC12SC;     // Enable conversions, Start conversion-software trigger 
  	while (!(ADC12IFGR0 & BIT0));
	XL_to_sink_data.data[0] = ADC12MEM0;
	while (!(ADC12IFGR0 & BIT1));
	XL_to_sink_data.data[1] = ADC12MEM1;
	while (!(ADC12IFGR0 & BIT2));
	XL_to_sink_data.data[2] = ADC12MEM2;	
}

void sink() {

}

/** Each node has a function to evaluate the dependancies, gets all incoming edges */
/*	and if all are satisfied, then returns true, assumes AND of all edges dependancies */
uint8_t __XL_dependancies_satisfied() {
	return 1;
}

uint8_t __sink_dependancies_satisfied() {
	return 1;
}

uint8_t dependancies_satisfied(uint8_t node_id) {
	switch(node_id) {
		case 0:
			return __XL_dependancies_satisfied();
		case 1:
			return __sink_dependancies_satisfied();
	}
	return 0;
}

void mayfly_init() {
	// Update all checkpoints, and timing information
}

void mayfly_main_loop() {
	for(i=0;i<NUM_NODES;i++) {
      // If all dependancies for this node are satisfied, execute it and break
      if(dependancies_satisfied(priority_list[i])) {
        current_node_id = priority_list[i];
        node_status[current_node_id] = STATE_DEPENDANCIES_SATISFIED;

        switch(current_node_id) {
          case 0:
            XL();
            // Task completed, checkpoint, add timing information
            XL_to_sink_data.timestamp[0] = current_time;
            break;
          case 1:
            sink();
            // Task completed, checkpoint, add timing information
            break;
        }
        node_status[current_node_id] = STATE_NODE_COMPLETE;       
        break;
      }
      // Else continue around to find a node that is satisfied. Note that source nodes will not always have deps
      // satisfied, as they are bound by hardware requirements (accelerometer can only )
    }
}