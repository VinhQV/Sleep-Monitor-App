#pragma once
#include <Arduino.h>
struct __attribute__((packed)) LampPacket {
  uint32_t magic=0x534C5039;
  uint8_t version=1,type=1; // 1=set, 2=application acknowledgment
  uint32_t boot=0,sequence=0;
  uint8_t brightness=0;
};
bool beginRadio();
bool sendRadio(const LampPacket& packet);
bool receiveRadio(LampPacket& packet);
