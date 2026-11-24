#include <cassert>
#include <cstdio>
#include "SleepRules.h"
#include "Automation.h"
static uint32_t simulatedMillis=0;
uint32_t millis(){return simulatedMillis;}
int main(){
  SleepRules r;r.alarmEnabled=true;r.alarmMinute=5;r.fadeMinutes=15;
  assert(sunriseBrightness(23*60+49,r)==-1);
  assert(sunriseBrightness(23*60+50,r)==0);
  assert(sunriseBrightness(0,r)==66);
  assert(sunriseBrightness(5,r)==100);
  assert(sunriseBrightness(6,r)==-1);
  r.alarmEnabled=false;assert(sunriseBrightness(5,r)==-1);
  Automation a;a.desired=60;a.rules.fadeMinutes=1;a.sleep(true);
  simulatedMillis=30000;a.tick(0,false);assert(a.desired==30);
  simulatedMillis=60000;a.tick(0,false);assert(a.desired==0);
  a.sleep(false);assert(a.desired==100);
  a.manual(42,0);simulatedMillis=120000;a.tick(0,false);assert(a.desired==42);
  a.rules.alarmEnabled=true;a.rules.timezoneOffset=0;a.rules.alarmMinute=5;a.rules.fadeMinutes=15;
  a.tick(0,true);assert(a.desired==66);
  a.manual(10,0);a.tick(300000,true);assert(a.desired==10);
  a.tick(360000,true);a.tick(86400000,true);assert(a.desired==66);
  std::puts("PASS: 13 assertions - midnight sunrise, fade, wake, manual override, next-day rearm");
}
