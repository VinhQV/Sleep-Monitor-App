#include "DeviceConfig.h"
#include <WiFi.h>
#include <WebServer.h>
#include <Preferences.h>
#include <esp_wifi.h>
DeviceConfig deviceConfig;
static uint32_t attemptAt=0,lastRetry=0;
static bool connecting=false;
static String escapeHtml(String s){s.replace("&","&amp;");s.replace("<","&lt;");s.replace(">","&gt;");s.replace("\"","&quot;");s.replace("'","&#39;");return s;}
bool parseMac(const String& s,uint8_t* b){unsigned x[6];char extra; if(sscanf(s.c_str(),"%2x:%2x:%2x:%2x:%2x:%2x%c",&x[0],&x[1],&x[2],&x[3],&x[4],&x[5],&extra)!=6||s.length()!=17)return false;for(int i=0;i<6;i++)b[i]=x[i];return !(b[0]&1);}
bool parseKey(const String& s,uint8_t* b){if(s.length()!=32)return false;for(int i=0;i<16;i++){char part[3]={s[i*2],s[i*2+1],0};if(!isxdigit(part[0])||!isxdigit(part[1]))return false;b[i]=strtoul(part,nullptr,16);}return true;}
static void load(){Preferences p;p.begin("network",true);deviceConfig.ssid=p.getString("ssid");deviceConfig.password=p.getString("password");deviceConfig.server=p.getString("server");deviceConfig.mqttPassword=p.getString("mqtt","sleep-device-demo");deviceConfig.peerMac=p.getString("peer");deviceConfig.linkKey=p.getString("key");deviceConfig.channel=p.getInt("channel",6);p.end();}
static void portal(const char* role){
  WiFi.mode(WIFI_AP_STA);String ownMac=WiFi.macAddress();String ap="Sleep-"+String(role)+"-"+ownMac.substring(12);ap.replace(":","");WiFi.softAP(ap.c_str(),AP_PASSWORD,deviceConfig.channel);
  WebServer web(80);bool saved=false;
  web.on("/",HTTP_GET,[&](){
    String h="<!doctype html><meta name='viewport' content='width=device-width'><title>Sleep Monitor Setup</title><style>body{font:16px system-ui;max-width:520px;margin:30px auto;padding:18px;background:#f5f7fc}input,button{box-sizing:border-box;width:100%;padding:12px;margin:6px 0 16px}button{background:#686ee5;color:white;border:0}</style><h1>Sleep Monitor</h1><p>Role: "+String(role)+"</p><p>STA MAC: <b>"+ownMac+"</b></p><form method='POST' action='/save'>";
    auto field=[&](const char* name,const char* label,String val,const char* type){h+="<label>"+String(label)+"<input required name='"+name+"' type='"+type+"' value='"+escapeHtml(val)+"'></label>";};
    field("ssid","Wi-Fi 2.4 GHz SSID",deviceConfig.ssid,"text");field("password","Wi-Fi password",deviceConfig.password,"password");field("server","Server IPv4 (no http://)",deviceConfig.server,"text");field("mqtt","MQTT device password",deviceConfig.mqttPassword,"password");field("peer","Other ESP32 STA MAC",deviceConfig.peerMac,"text");field("key","Shared ESP-NOW key (32 hex characters)",deviceConfig.linkKey,"password");field("channel","Router channel (1..13, same for both boards)",String(deviceConfig.channel),"number");
    h+="<button>Save and restart</button></form><p>For initial MAC discovery, open each board setup page before filling peer MAC. Both must use the same Wi-Fi channel and shared key.</p>";web.send(200,"text/html; charset=utf-8",h);
  });
  web.on("/save",HTTP_POST,[&](){uint8_t m[6],k[16];IPAddress ip;int ch=web.arg("channel").toInt();
    if(web.arg("ssid").isEmpty()||web.arg("ssid").length()>32||web.arg("password").length()>63||web.arg("mqtt").isEmpty()||!ip.fromString(web.arg("server"))||!parseMac(web.arg("peer"),m)||!parseKey(web.arg("key"),k)||ch<1||ch>13){web.send(400,"text/plain","Invalid fields. Use IPv4, unicast STA MAC, 32 hex key and channel 1..13.");return;}
    Preferences p;p.begin("network",false);p.putString("ssid",web.arg("ssid"));p.putString("password",web.arg("password"));p.putString("server",web.arg("server"));p.putString("mqtt",web.arg("mqtt"));p.putString("peer",web.arg("peer"));p.putString("key",web.arg("key"));p.putInt("channel",ch);p.end();web.send(200,"text/plain","Saved. Restarting; reconnect to your normal Wi-Fi.");saved=true;
  });
  web.begin();Serial.printf("Setup AP: %s, open http://192.168.4.1 | STA MAC: %s\n",ap.c_str(),ownMac.c_str());
  while(!saved){web.handleClient();delay(5);}delay(500);ESP.restart();
}
void beginNetwork(const char* role){
  pinMode(BUTTON_PIN,INPUT_PULLUP);pinMode(STATUS_LED,OUTPUT);WiFi.persistent(false);WiFi.mode(WIFI_STA);WiFi.setSleep(false);WiFi.setAutoReconnect(false);load();
  Serial.printf("%s STA MAC: %s\n",role,WiFi.macAddress().c_str());uint8_t m[6],k[16];
  if(deviceConfig.ssid.isEmpty()||!parseMac(deviceConfig.peerMac,m)||!parseKey(deviceConfig.linkKey,k)||digitalRead(BUTTON_PIN)==LOW)portal(role);
  WiFi.begin(deviceConfig.ssid.c_str(),deviceConfig.password.c_str(),deviceConfig.channel);attemptAt=lastRetry=millis();connecting=true;
}
void tickNetwork(){
  if(WiFi.status()==WL_CONNECTED){connecting=false;digitalWrite(STATUS_LED,HIGH);return;}
  digitalWrite(STATUS_LED,(millis()/500)%2);
  if(connecting&&millis()-attemptAt>6000){WiFi.disconnect();esp_wifi_set_channel(deviceConfig.channel,WIFI_SECOND_CHAN_NONE);connecting=false;}
  if(!connecting&&millis()-lastRetry>60000){WiFi.begin(deviceConfig.ssid.c_str(),deviceConfig.password.c_str(),deviceConfig.channel);attemptAt=lastRetry=millis();connecting=true;}
}
