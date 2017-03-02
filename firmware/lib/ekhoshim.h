#ifndef EKHOSHIM_H
#define EKHOSHIM_H

#define INVISIBLE_MEM_MAIN_BUFFER 0x81000
#define DEBUG_BUFFER 0x82000

static inline void send_id (char id)
{
  unsigned long *__mspsim_debug_mem = (unsigned long *) DEBUG_BUFFER;
  *__mspsim_debug_mem = id;
}


static 	inline void main_start (char counter) {
  unsigned long * __mspsim_main_mem = (unsigned long *) INVISIBLE_MEM_MAIN_BUFFER;
  *__mspsim_main_mem = counter;
}

#endif // EKHOSHIM_H