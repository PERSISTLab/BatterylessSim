#include "mayfly_program.h"
#include "../../../lib/ekhoshim.h"

const int priority_list[2] = {1, 0};
const int source_node = 0;
volatile int i;
volatile int next;
volatile int size_buf;
typedef struct 
{
  int max_samples;
  int sample_size;
  int data[3];
  int timestamp[1];
  int head;
  int tail;
  int samples_in_buf;
} __XL_to_sink_data_t;
__XL_to_sink_data_t XL_to_sink_data = {1, 3, {0}, {0}, 0, 0, 0};
typedef struct 
{
  int magic_number;
  int current_node_id;
  int node_status[2];
} mayfly_state_t;
mayfly_state_t __mayfly_state = {0, 1, {0}};
void XL()
{
  ADC12CTL0 &= ~ADC12ENC;
  ADC12CTL0 = (ADC12ON | ADC12SHT0_2) | ADC12MSC;
  ADC12CTL1 = ADC12SHP | ADC12CONSEQ_1;
  ADC12MCTL0 = ADC12INCH_1;
  ADC12MCTL1 = ADC12INCH_2;
  ADC12MCTL2 = ADC12INCH_3 | ADC12EOS;
  ADC12CTL0 |= ADC12ENC | ADC12SC;
  while (!(ADC12IFGR0 & BIT0)){} 
    ;

  XL_to_sink_data.data[0] = ADC12MEM0;
  while (!(ADC12IFGR0 & BIT1)){}
    ;

  XL_to_sink_data.data[1] = ADC12MEM1;
  while (!(ADC12IFGR0 & BIT2)){}
    ;

  XL_to_sink_data.data[2] = ADC12MEM2;
  XL_to_sink_data.samples_in_buf = 1;
  __delay_cycles(10000);
}

void sink()
{
  __delay_cycles(10000);
}

int __XL_dependancies_satisfied()
{
  return 1;
}

int __sink_dependancies_satisfied()
{
  if (XL_to_sink_data.samples_in_buf > 0)
  {
    return 1;
  }

  return 0;
}

int dependancies_satisfied(int node_id)
{
  int retval = 0;
  switch (node_id)
  {
    case 0:
      retval = __XL_dependancies_satisfied();
      break;

    case 1:
      retval = __sink_dependancies_satisfied();
      break;

  }

  return retval;
}

void mayfly_init(int current_time)
{
  if (__mayfly_state.magic_number != 0xdeedbeef)
  {
    XL_to_sink_data.samples_in_buf = 0;
  }

}

void mayfly_schedule(int current_time)
{
  for (i = 0; i < 2; i++)
  {
    if (dependancies_satisfied(priority_list[i]))
    {
      __mayfly_state.current_node_id = priority_list[i];
      __mayfly_state.magic_number = 0;
      switch (__mayfly_state.current_node_id)
      {
        case 0:
          XL();
          XL_to_sink_data.timestamp[0] = current_time;
          break;

        case 1:
          sink();
          XL_to_sink_data.samples_in_buf = 0;
          break;

      }

      __mayfly_state.magic_number = 0xdeedbeef;
      break;
    }

  }

}
