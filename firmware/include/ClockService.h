#pragma once
#include <stdint.h>
void beginClock();
void tickClock();
uint64_t nowMs();
bool clockReady();
