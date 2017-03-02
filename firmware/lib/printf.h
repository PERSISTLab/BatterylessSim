#ifndef PRINTF_MSP430_H
#define PRINTF_MSP430_H

/**
 * This name is unique: it is ignored by the cycle counter of mspsim and energy counter
 * Otherwise functions as a normal printf: for most nodes this is forwarded to the console.
 */
int siren_command(const char *format, ...);

#endif // PRINTF_MSP430_H
