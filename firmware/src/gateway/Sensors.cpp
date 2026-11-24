#include "Sensors.h"
#include <Wire.h>
#include <Adafruit_SHT31.h>
#include <BH1750.h>
static Adafruit_SHT31 sht;static BH1750 lux;static bool shtReady=false,luxReady=false;
bool beginSensors(){Wire.begin(21,22);Wire.setTimeOut(100);shtReady=sht.begin(0x44);luxReady=lux.begin(BH1750::CONTINUOUS_HIGH_RES_MODE,0x23);return shtReady&&luxReady;}
bool readSensors(JsonDocument& d){
  if(!shtReady)shtReady=sht.begin(0x44);
  if(!luxReady)luxReady=lux.begin(BH1750::CONTINUOUS_HIGH_RES_MODE,0x23);
  float t=shtReady?sht.readTemperature():NAN,h=shtReady?sht.readHumidity():NAN,l=luxReady?lux.readLightLevel():-1;
  d["temperature"]=nullptr;d["humidity"]=nullptr;d["light"]=nullptr;
  if(isfinite(t)&&t>=-40&&t<=85)d["temperature"]=t;
  if(isfinite(h)&&h>=0&&h<=100)d["humidity"]=h;
  if(isfinite(l)&&l>=0)d["light"]=l;
  shtReady=isfinite(t)&&isfinite(h);luxReady=isfinite(l)&&l>=0;
  return shtReady&&luxReady;
}
