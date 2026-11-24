#pragma once
#include <Arduino.h>
bool beginMicrophone();
// Hold the external button to record, release to send. Serial mode records 6 s.
size_t recordWav(uint8_t* buffer, size_t capacity, bool serialMode);
