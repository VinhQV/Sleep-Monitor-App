#pragma once
#include "SleepRules.h"
#include <Arduino.h>
class Automation {
  bool fading=false,suppressSunrise=false;uint32_t fadeStart=0;int fadeFrom=0;
public:
  SleepRules rules;
  bool sleeping=false;
  int desired=0;
  void sleep(bool value){if(value==sleeping)return;sleeping=value;suppressSunrise=false;if(rules.autoLight){fading=value;fadeStart=millis();fadeFrom=desired;if(!value)desired=100;}}
  void manual(int level,uint64_t now){desired=level;fading=false;int m=(now/60000+rules.timezoneOffset)%1440;suppressSunrise=sunriseBrightness(m,rules)>=0;}
  void tick(uint64_t now,bool valid){
    if(fading){uint32_t elapsed=millis()-fadeStart,duration=rules.fadeMinutes*60000UL;desired=elapsed>=duration?0:fadeFrom-(uint64_t)fadeFrom*elapsed/duration;if(elapsed>=duration)fading=false;}
    if(valid){int sec=(now/1000+rules.timezoneOffset*60LL)%86400;int start=(rules.alarmMinute*60-rules.fadeMinutes*60+86400)%86400;int elapsed=(sec-start+86400)%86400;
      if(elapsed>rules.fadeMinutes*60){suppressSunrise=false;return;}
      if(rules.alarmEnabled&&!suppressSunrise){desired=elapsed*100/(rules.fadeMinutes*60);fading=false;}
    }
  }
};
