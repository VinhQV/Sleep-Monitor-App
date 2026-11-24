#include <Arduino.h>
#include <Preferences.h>
#include "DeviceConfig.h"
#include "RadioLink.h"
constexpr int PWM_PIN=25,PWM_CHANNEL=0;
static uint32_t lastBoot=0,lastSequence=0,lastSave=0;static uint8_t brightness=0,savedBrightness=0;
static Preferences prefs;
void setup(){
  Serial.begin(115200);ledcSetup(PWM_CHANNEL,5000,10);ledcAttachPin(PWM_PIN,PWM_CHANNEL);ledcWrite(PWM_CHANNEL,0);
  prefs.begin("lamp",false);brightness=savedBrightness=prefs.getUChar("level",0);ledcWrite(PWM_CHANNEL,(uint32_t)brightness*1023/100);
  beginNetwork("lamp");if(!beginRadio())Serial.println("ESP-NOW initialization failed. Check pairing settings.");
}
void loop(){
  tickNetwork();LampPacket p;
  while(receiveRadio(p)){
    if(p.type!=1)continue;
    // Duplicate requests are idempotent and re-acknowledged; old sequences cannot restore an older level.
    if(p.boot==lastBoot&&p.sequence<lastSequence)continue;
    lastBoot=p.boot;lastSequence=p.sequence;brightness=p.brightness;
    ledcWrite(PWM_CHANNEL,(uint32_t)brightness*1023/100);
    p.type=2;sendRadio(p); // Acknowledges that PWM was applied, not a measured light output.
  }
  if(brightness!=savedBrightness&&millis()-lastSave>30000){prefs.putUChar("level",brightness);savedBrightness=brightness;lastSave=millis();}
  delay(5);
}
