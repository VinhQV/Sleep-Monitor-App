#include "ClockService.h"
#include "DeviceConfig.h"
#include <Arduino.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <sys/time.h>
static uint32_t lastTry=0;
void beginClock(){configTime(0,0,"pool.ntp.org","time.google.com");}
uint64_t nowMs(){timeval tv;gettimeofday(&tv,nullptr);return (uint64_t)tv.tv_sec*1000+tv.tv_usec/1000;}
bool clockReady(){return nowMs()>1704067200000ULL;}
void tickClock(){
  if(clockReady()||WiFi.status()!=WL_CONNECTED||millis()-lastTry<10000)return;
  lastTry=millis();HTTPClient http;http.setConnectTimeout(1500);http.setTimeout(1500);http.begin("http://"+deviceConfig.server+":8080/api/time");
  if(http.GET()==200){StaticJsonDocument<128>d;if(!deserializeJson(d,http.getString())){uint64_t ms=d["timestamp"]|0ULL;if(ms>1704067200000ULL){timeval tv={(time_t)(ms/1000),(suseconds_t)((ms%1000)*1000)};settimeofday(&tv,nullptr);}}}http.end();
}
