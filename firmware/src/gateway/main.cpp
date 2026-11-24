#include <Arduino.h>
#include <WiFi.h>
#include <Preferences.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include "DeviceConfig.h"
#include "RadioLink.h"
#include "DurableQueue.h"
#include "Sensors.h"
#include "ClockService.h"
#include "Automation.h"

static WiFiClient network;static PubSubClient mqtt(network);static Preferences prefs;
static DurableQueue queue;static Automation automation;
static uint32_t bootId,sequence=0,radioSequence=0,lastRadio=0,lastAck=0,lastSample=0,lastStatus=0,lastMqtt=0,lastQueueSend=0;
static int reportedBrightness=0,lastDesired=-1;static uint32_t dropped=0;
static bool sensorWasHealthy=true,wasOutside=false,configDeferred=false;
static String deferredConfig,pendingCommand;static uint64_t commandExpires=0;static uint32_t commandSequence=0;
static LampPacket outgoing;
static String topic(const char* leaf){return "sleep/"+String(DEVICE_ID)+"/"+leaf;}
static String newId(){char b[70];snprintf(b,sizeof(b),"%08x_%08x_%08x",(uint32_t)ESP.getEfuseMac(),bootId,++sequence);return b;}
static bool publish(const char* leaf,const JsonDocument& d){String data;serializeJson(d,data);return mqtt.publish(topic(leaf).c_str(),data.c_str());}
static bool record(const char* leaf,JsonDocument& d,bool critical=false){
  if(!clockReady()){Serial.println("Record skipped: clock not synchronized.");return false;}
  String id=newId();d["id"]=id;d["timestamp"]=nowMs();d["source"]="device";String data;serializeJson(d,data);
  bool ok=queue.push(topic(leaf),data,id,critical);if(!ok){dropped++;Serial.println("Queue full/unavailable: record not saved.");}return ok;
}
static bool event(const char* kind,String value){StaticJsonDocument<512>d;d["kind"]=kind;d["value"]=value;return record("event",d,true);}
static void sleepState(bool value){automation.sleep(value);prefs.putBool("sleeping",value);}
static void loadRules(JsonDocument& d){
  SleepRules& r=automation.rules;
  r.autoLight=d["autoLight"]|true;r.alarmEnabled=d["alarmEnabled"]|false;r.alarmMinute=constrain(d["alarmMinute"]|390,0,1439);r.fadeMinutes=constrain(d["fadeMinutes"]|15,1,60);r.timezoneOffset=constrain(d["timezoneOffset"]|420,-720,840);
  r.temperatureMin=d["temperatureMin"]|18.0;r.temperatureMax=d["temperatureMax"]|27.0;r.humidityMin=d["humidityMin"]|40.0;r.humidityMax=d["humidityMax"]|70.0;r.lightMax=d["lightMax"]|150.0;
}
static void applyConfig(JsonDocument& d){
  loadRules(d);String raw;serializeJson(d,raw);if(raw!=prefs.getString("rules"))prefs.putString("rules",raw);
  if(queue.size()){configDeferred=true;deferredConfig=raw;}else if(d.containsKey("sleeping"))sleepState(d["sleeping"]);
}
static void commandAck(bool ok,const char* detail){
  if(pendingCommand.isEmpty())return;
  StaticJsonDocument<256>d;d["id"]=pendingCommand;d["ok"]=ok;d["detail"]=detail;
  if(publish("command-ack",d))pendingCommand="";
}
static void prepareRadio(){outgoing=LampPacket();outgoing.boot=bootId;outgoing.sequence=++radioSequence;outgoing.brightness=automation.desired;lastDesired=automation.desired;lastRadio=millis();sendRadio(outgoing);}
static void message(char* t,uint8_t* bytes,unsigned len){
  StaticJsonDocument<1536>d;if(deserializeJson(d,bytes,len))return;String name=t;
  if(name==topic("ack")){
    if(d["ok"]==true)queue.ack(d["id"].as<String>());
    else{Serial.printf("Server rejected %s: %s\n",d["id"]|"",d["error"]|"");queue.ack(d["id"].as<String>());dropped++;}
    // Immediately permit the next durable item after an application-level acknowledgment.
    lastQueueSend=0;
  }else if(name==topic("config"))applyConfig(d);
  else if(name==topic("command")){
    const char* id=d["id"]|"";uint64_t expires=d["expiresAt"]|0ULL;int level=d["brightness"]|-1;
    if(!clockReady()||expires<nowMs()||expires-nowMs()>20000||strlen(id)!=36||level<0||level>100)return;
    if(pendingCommand.length())commandAck(false,"Superseded by another command");
    pendingCommand=id;commandExpires=expires;automation.manual(level,nowMs());prepareRadio();commandSequence=outgoing.sequence;
  }
}
static void connectMqtt(){
  if(WiFi.status()!=WL_CONNECTED||mqtt.connected()||millis()-lastMqtt<5000)return;
  lastMqtt=millis();String clientId=String(DEVICE_ID)+"-"+String(bootId,HEX);
  if(mqtt.connect(clientId.c_str(),DEVICE_ID,deviceConfig.mqttPassword.c_str(),topic("status").c_str(),1,false,"{\"online\":false,\"source\":\"device\"}")){
    mqtt.subscribe(topic("ack").c_str(),1);mqtt.subscribe(topic("config").c_str(),1);mqtt.subscribe(topic("command").c_str(),1);Serial.println("MQTT connected");lastStatus=0;
  }
}
static void tickButton(){
  static bool raw=true,stable=true;static uint32_t changed=0;
  bool read=digitalRead(BUTTON_PIN);if(read!=raw){raw=read;changed=millis();}
  if(millis()-changed>40&&stable!=raw){stable=raw;if(!stable){
    bool next=!automation.sleeping;
    if(event(next?"sleep_start":"sleep_end","physical_button")){sleepState(next);Serial.println(next?"Sleep started":"Sleep ended");}
    else Serial.println("Button action not recorded. Check clock and filesystem.");
  }}
}
static void sample(){
  uint32_t interval=mqtt.connected()?15000:60000;if(millis()-lastSample<interval)return;lastSample=millis();
  StaticJsonDocument<512>d;bool healthy=readSensors(d);
  if(healthy!=sensorWasHealthy){event("sensor_error",healthy?"Sensors recovered":"Missing SHT31 or BH1750; missing values are null");sensorWasHealthy=healthy;}
  const auto& r=automation.rules;bool outside=healthy&&(d["temperature"].as<float>()<r.temperatureMin||d["temperature"].as<float>()>r.temperatureMax||d["humidity"].as<float>()<r.humidityMin||d["humidity"].as<float>()>r.humidityMax||d["light"].as<float>()>r.lightMax);
  if(outside&&!wasOutside)event("environment_outside","Room values exceeded configured thresholds");
  wasOutside=outside;record("telemetry",d);
}
static void tickRadio(){
  if(automation.desired!=lastDesired)prepareRadio();
  if(millis()-lastRadio>1000){lastRadio=millis();sendRadio(outgoing);}
  LampPacket p;while(receiveRadio(p))if(p.type==2&&p.boot==bootId&&p.sequence==outgoing.sequence&&p.brightness==outgoing.brightness){
    lastAck=millis();reportedBrightness=p.brightness;
    if(pendingCommand.length()&&p.sequence==commandSequence){commandAck(true,"PWM applied by lamp node");event("light_change",String(reportedBrightness)+"% (manual command)");}
  }
  if(pendingCommand.length()&&clockReady()&&nowMs()>commandExpires)commandAck(false,"Lamp response timeout");
}
static void status(){
  if(!mqtt.connected()||millis()-lastStatus<5000)return;
  lastStatus=millis();
  StaticJsonDocument<512>d;d["online"]=true;d["source"]="device";d["lampOnline"]=lastAck>0&&millis()-lastAck<5000;d["brightness"]=reportedBrightness;d["timeSynced"]=clockReady();d["queue"]=queue.size();d["dropped"]=dropped;d["storageReady"]=queue.healthy();publish("status",d);
}
void setup(){
  Serial.begin(115200);bootId=esp_random();prefs.begin("gateway",false);automation.sleeping=prefs.getBool("sleeping",false);
  StaticJsonDocument<1536>d;if(!deserializeJson(d,prefs.getString("rules","{}")))loadRules(d);
  beginNetwork("gateway");queue.begin();beginSensors();beginClock();
  if(!beginRadio())Serial.println("ESP-NOW failed; inspect MAC/key settings.");
  mqtt.setServer(deviceConfig.server.c_str(),1883);mqtt.setBufferSize(2048);mqtt.setCallback(message);mqtt.setSocketTimeout(1);mqtt.setKeepAlive(20);
  prepareRadio();
}
void loop(){
  tickNetwork();tickClock();connectMqtt();mqtt.loop();tickButton();automation.tick(nowMs(),clockReady());tickRadio();sample();status();
  if(mqtt.connected()&&millis()-lastQueueSend>1000){QueueItem q;if(queue.front(q)){mqtt.publish(q.topic.c_str(),q.payload.c_str());lastQueueSend=millis();}}
  if(configDeferred&&queue.size()==0&&mqtt.connected()){
    // Re-read retained server state after offline button events have been replayed.
    configDeferred=false;mqtt.subscribe(topic("config").c_str(),1);
  }
  delay(5);
}
