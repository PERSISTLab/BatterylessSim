#ifndef MAYFLY_PROGRAM_H
#define MAYFLY_PROGRAM_H

#include <stdint.h>
#include <string.h>
#include <msp430.h>
#include "../../../lib/printf.h"
/** Node function definitions from user code + mayfly additions */
void XL();
void sink2();


int __XL_dependancies_satisfied();
int __sink2_dependancies_satisfied();

int dependancies_satisfied(int node_id);

void mayfly_init(int current_time);
void mayfly_schedule(int current_time);

#endif // MAYFLY_PROGRAM_H