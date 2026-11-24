#include <Arduino.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <time.h>
#include <HTTPClient.h>
#include <Wire.h>
#include <Adafruit_SHT31.h>
#include <ArduinoJson.h>
#if __has_include("secrets.h")
#include "secrets.h"
#else
#include "VoiceConfig.example.h"
#endif

#include "SetupPortal.h"
#include "ServerTrust.h"

namespace {
constexpr uint8_t kLightPin = D0;  // ADC1 GPIO1, module AO
constexpr uint32_t kSampleMs = 5000;
Adafruit_SHT31 sht(&Wire);
String server;
bool shtReady = false;
bool setupOffered = false;
bool statusPending = true;
bool lastWifiConnected = false;
uint32_t lastSample = 0;
uint32_t lastStatus = 0;
uint32_t lastReconnect = 0;
String serialCommand;

void readConfiguration(){
  while(Serial.available()){
    const char c=Serial.read();
    if(c=='\n'){
      StaticJsonDocument<512> command;
      const bool valid=!deserializeJson(command,serialCommand);
      if(valid && command["diagnose"]==true){
        Serial.printf("Wi-Fi diagnostic: ssid=%s, status=%d, RSSI=%d, portal=%d\n",SetupPortal::ssid.c_str(),int(WiFi.status()),WiFi.RSSI(),SetupPortal::open);
        if(WiFi.status()!=WL_CONNECTED){
          const int count=WiFi.scanNetworks();int matches=0;
          for(int i=0;i<count;i++)if(WiFi.SSID(i)==SetupPortal::ssid){
            ++matches;Serial.printf("Configured network visible: channel=%d, RSSI=%d, security=%d\n",WiFi.channel(i),WiFi.RSSI(i),int(WiFi.encryptionType(i)));
          }
          Serial.printf("Configured network matches: %d\n",matches);WiFi.scanDelete();
        }
      }else if(valid&&command["server"].is<const char*>()&&command["key"].is<const char*>()){
        if(SetupPortal::configureServer(command["server"].as<String>(),command["key"].as<String>())){
          if(command["ssid"].is<const char*>()){
            if(SetupPortal::configureWifi(command["ssid"].as<String>(),command["password"]|""))Serial.println("Wi-Fi configuration saved");
            else Serial.println("Wi-Fi configuration rejected");
          }
          statusPending=true;lastStatus=0;lastSample=millis()-kSampleMs;
          if(SetupPortal::open)SetupPortal::stop();
          Serial.println("Remote HTTPS configuration saved");
          Serial.flush();delay(200);ESP.restart();
        }else Serial.println("Remote configuration rejected");
      }
      serialCommand="";
    }else if(c!='\r'){
      if(serialCommand.length()<500)serialCommand+=c;
      else serialCommand="";
    }
  }
}

bool postJson(const char* path, const String& body) {
  WiFiClient client;
  WiFiClientSecure secure;
  HTTPClient http;
  http.useHTTP10(true);
  http.setReuse(false);
  http.setConnectTimeout(5000);
  http.setTimeout(8000);
  const bool tls=server.startsWith("https://");
  if(tls && time(nullptr)<1700000000){Serial.println("Waiting for network time before HTTPS");return false;}
  secure.setCACert(kServerRootCertificates);
  secure.setHandshakeTimeout(8);
  if (!(tls?http.begin(secure, server+path):http.begin(client, server+path))) return false;
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-API-Key", SetupPortal::key);
  const int status = http.POST(body);
  if (status != 200) {
    Serial.printf("HTTP %d: %s\n", status,
      status < 0 ? HTTPClient::errorToString(status).c_str() : "server rejected request");
  }
  http.end();
  return status == 200;
}

void sampleSensors() {
  StaticJsonDocument<256> doc;
  doc["temperature"] = nullptr;
  doc["humidity"] = nullptr;
  if (!shtReady) shtReady = sht.begin(0x44) || sht.begin(0x45);
  float temperature = NAN, humidity = NAN;
  if (shtReady && sht.readBoth(&temperature, &humidity) &&
      isfinite(temperature) && isfinite(humidity) &&
      temperature >= -40 && temperature <= 85 && humidity >= 0 && humidity <= 100) {
    doc["temperature"] = temperature;
    doc["humidity"] = humidity;
  } else {
    shtReady = false;
    Serial.println("SHT30 unavailable; ADC remains active");
  }
  uint32_t raw = 0, millivolts = 0;
  for (int i = 0; i < 16; ++i) {
    raw += analogRead(kLightPin);
    millivolts += analogReadMilliVolts(kLightPin);
    delay(1);
  }
  doc["lightAdc"] = (raw + 8) / 16;
  doc["lightMillivolts"] = min(uint32_t(3300), (millivolts + 8) / 16);
  String payload;
  serializeJson(doc, payload);
  Serial.println(payload);
  if (postJson("/api/device/environment", payload)) Serial.println("Sensors synced");
  else statusPending=true;
}
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  pinMode(kLightPin, INPUT);
  pinMode(LED_BUILTIN, OUTPUT);
  // A physical switch interrupts BAT+. Whenever power is present, run normally.
  // D1 is unused; battery voltage must never be connected to this GPIO.
  digitalWrite(LED_BUILTIN, LOW);
  lastSample = millis() - kSampleMs;
  analogReadResolution(12);
  analogSetPinAttenuation(kLightPin, ADC_11db);
  Wire.begin(D4, D5, 100000);
  Wire.setTimeOut(100);
  server = VOICE_SERVER_URL;
  while (server.endsWith("/")) server.remove(server.length() - 1);
  WiFi.mode(WIFI_STA);
  WiFi.setSleep(false);
  WiFi.setAutoReconnect(true);
  WiFi.onEvent([](WiFiEvent_t, WiFiEventInfo_t info) {
    Serial.printf("Wi-Fi disconnected, reason=%u\n", info.wifi_sta_disconnected.reason);
  }, ARDUINO_EVENT_WIFI_STA_DISCONNECTED);
  SetupPortal::begin(VOICE_WIFI_SSID, VOICE_WIFI_PASSWORD, VOICE_SERVER_URL, VOICE_SERVER_KEY);
  Serial.println("SHT30 SDA=D4 SCL=D5; light AO=D0; D1 unused");
  Serial.println("Power-switch firmware: powered ON; switch interrupts BAT+ (USB still powers board)");
}

void loop() {
  readConfiguration();
  SetupPortal::tick();
  server=SetupPortal::url;
  if(SetupPortal::open){delay(2);return;}
  if(WiFi.status()==WL_CONNECTED)setupOffered=true;
  if(!setupOffered && millis()>60000){setupOffered=true;SetupPortal::start();return;}
  const bool connected = WiFi.status() == WL_CONNECTED;
  if (connected && !lastWifiConnected) {
    statusPending = true;
    lastStatus=millis()-kSampleMs;
    configTime(0,0,"time.google.com","pool.ntp.org");
    Serial.printf("Wi-Fi connected: %s\n", WiFi.localIP().toString().c_str());
  }
  lastWifiConnected = connected;
  if (!connected) {
    if (millis() - lastReconnect > 15000) {
      lastReconnect = millis();
      WiFi.reconnect();
    }
    delay(5);
    return;
  }
  if (statusPending) {
    if(millis()-lastStatus<kSampleMs){delay(5);return;}
    lastStatus = millis();
    // Announce ON before telemetry, including after a reboot from legacy OFF.
    // Power loss cannot send OFF; the server expires this heartbeat after 15 s.
    const bool ok = postJson("/api/device/status", "{\"enabled\":true}");
    statusPending = !ok;
    if (ok) Serial.println("Heartbeat synced (powered ON)");
    if (!ok) { delay(50); return; }
  }
  if (!statusPending && millis() - lastSample >= kSampleMs) {
    lastSample = millis();
    sampleSensors();
  }
  delay(5);
}
