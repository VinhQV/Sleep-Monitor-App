#pragma once
#include <stdint.h>
struct SleepRules {
  bool autoLight=true,alarmEnabled=false;
  int alarmMinute=390,fadeMinutes=15,timezoneOffset=420;
  double temperatureMin=18,temperatureMax=27,humidityMin=40,humidityMax=70,lightMax=150;
};
// -1 outside the sunrise interval. Supports intervals crossing midnight.
inline int sunriseBrightness(int minute,const SleepRules& r){
  if(!r.alarmEnabled)return -1;
  int start=(r.alarmMinute-r.fadeMinutes+1440)%1440;
  int elapsed=(minute-start+1440)%1440;
  return elapsed<=r.fadeMinutes?elapsed*100/r.fadeMinutes:-1;
}
