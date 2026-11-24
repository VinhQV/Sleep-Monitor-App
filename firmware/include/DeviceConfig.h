#pragma once
#include <Arduino.h>
struct DeviceConfig {
  String ssid, password, server, mqttPassword, peerMac, linkKey;
  int channel=6;
};
extern DeviceConfig deviceConfig;
constexpr int BUTTON_PIN=32;
constexpr int STATUS_LED=2;
constexpr const char* DEVICE_ID="bedroom-01";
constexpr const char* AP_PASSWORD="SleepSetup9";
// Hold GPIO32 to ground while booting to reopen the protected setup AP.
void beginNetwork(const char* role);
void tickNetwork();
bool parseMac(const String& text,uint8_t* bytes);
bool parseKey(const String& text,uint8_t* bytes);
