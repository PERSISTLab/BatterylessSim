#ifndef MAYFLY_PROGRAM_H
#define MAYFLY_PROGRAM_H

#include <stdint.h>
#include <string.h>
#include <msp430.h>
#include "printf.h"
/** Node function definitions from user code + mayfly additions */
void XL();
void sink2();


uint8_t __XL_dependancies_satisfied();
uint8_t __sink2_dependancies_satisfied();

uint8_t dependancies_satisfied(uint8_t node_id);

void mayfly_init(uint32_t current_time);
void mayfly_schedule(uint32_t current_time);

#endif // MAYFLY_PROGRAM_H