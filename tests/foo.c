#include "msp430setup.h"
#include <stdio.h>

int main (void) {
    msp430_setup();

    int x = 10;
    int y = x + 5;

    printf("EXIT\n");
    return 0;
}
