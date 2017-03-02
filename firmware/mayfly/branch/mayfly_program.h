#ifndef MAYFLY_PROGRAM_H
#define MAYFLY_PROGRAM_H

#include <stdint.h>
#include <string.h>

/** Node function definitions from user code + mayfly additions */
void XL(uint16_t inputs[], uint16_t outputs[]);
void decide(uint16_t inputs[], uint16_t outputs[]);
void sink1(uint16_t inputs[], uint16_t outputs[]);
void sink2(uint16_t inputs[], uint16_t outputs[]);


uint8_t __XL_dependancies_satisfied();
uint8_t __decide_dependancies_satisfied();
uint8_t __sink1_dependancies_satisfied();
uint8_t __sink2_dependancies_satisfied();

uint8_t dependancies_satisfied(uint8_t node_id);

void mayfly_main_loop();

#endif // MAYFLY_PROGRAM_H