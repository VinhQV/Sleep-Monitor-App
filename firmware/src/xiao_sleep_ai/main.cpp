#include <Arduino.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <Wire.h>
#include <Adafruit_SHT31.h>
#include <ArduinoJson.h>
#include <esp_heap_caps.h>
#include "AudioCapture.h"
#if __has_include("secrets.h")
#include "secrets.h"
#else
#include "VoiceConfig.example.h"
#endif

namespace {
Adafruit_SHT31 sht(&Wire);
bool shtReady = false;
bool micReady = false;
bool buttonHeld = false;
uint8_t* audio = nullptr;
uint32_t lastSensor = 0;
uint32_t lastWifi = 0;
String server;

String requestId() {
  uint8_t bytes[16];
  esp_fill_random(bytes, sizeof(bytes));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  char id[37];
  snprintf(id, sizeof(id),
    "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
    bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6],
    bytes[7], bytes[8], bytes[9], bytes[10], bytes[11], bytes[12],
    bytes[13], bytes[14], bytes[15]);
  return String(id);
}

void sendEnvironment() {
  if (!shtReady) shtReady = sht.begin(0x44) || sht.begin(0x45);
  float temperature, humidity;
  if (!shtReady || !sht.readBoth(&temperature, &humidity) ||
      !isfinite(temperature) || !isfinite(humidity)) {
    shtReady = false;
    Serial.println("SHT30 unavailable");
    return;
  }
  StaticJsonDocument<128> doc;
  doc["temperature"] = temperature;
  doc["humidity"] = humidity;
  String payload;
  serializeJson(doc, payload);
  Serial.println(payload);
  HTTPClient http;
  http.useHTTP10(true);
  http.setReuse(false);
  http.setConnectTimeout(15000);
  http.setTimeout(15000);
  if (http.begin(server + "/api/device/environment")) {
    http.addHeader("X-API-Key", VOICE_SERVER_KEY);
    http.addHeader("Content-Type", "application/json");
    const int status = http.POST(payload);
    if (status != 200) Serial.printf("Environment HTTP error: %d (%s)\n", status,
      status < 0 ? HTTPClient::errorToString(status).c_str() : "server rejected request");
    else Serial.println("Environment synced");
    http.end();
  }
}

void askSleepAi(bool serialMode) {
  if (!micReady || !audio) {
    Serial.println("Microphone or PSRAM unavailable");
    return;
  }
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("Wi-Fi is disconnected");
    return;
  }
  Serial.println("Recording. Release button to send voice to Gemini Cloud.");
  digitalWrite(LED_BUILTIN, LOW);
  const size_t length = recordWav(audio, 256044, serialMode);
  digitalWrite(LED_BUILTIN, HIGH);
  if (!length) {
    Serial.println("Recording too short or failed. Hold and speak for 0.5-8 seconds.");
    return;
  }
  Serial.println("Sending voice to Sleep AI...");
  HTTPClient http;
  http.useHTTP10(true);
  http.setReuse(false);
  http.setConnectTimeout(15000);
  http.setTimeout(60000);
  if (!http.begin(server + "/api/ai/voice")) return;
  http.addHeader("X-API-Key", VOICE_SERVER_KEY);
  http.addHeader("X-Request-ID", requestId());
  http.addHeader("Content-Type", "audio/wav");
  const int status = http.POST(audio, length);
  if (status == 200) {
    DynamicJsonDocument reply(24000);
    if (deserializeJson(reply, http.getString()) == DeserializationError::Ok) {
      Serial.printf("You: %s\nSleep AI: %s\n",
        reply["transcript"] | "", reply["text"] | "");
      Serial.println("Open Sleep AI in the app to view this conversation.");
    } else Serial.println("Invalid server response");
  } else {
    Serial.printf("Voice request failed: HTTP %d\n", status);
    if (status > 0) {
      StaticJsonDocument<1024> error;
      if (!deserializeJson(error, http.getString())) Serial.println(error["error"] | "Server error");
    }
  }
  http.end();
  memset(audio, 0, length);
}
}

void setup() {
  Serial.begin(115200);
  delay(1500);
  pinMode(D1, INPUT_PULLUP);
  pinMode(LED_BUILTIN, OUTPUT);
  digitalWrite(LED_BUILTIN, HIGH);
  Wire.begin(D4, D5, 100000);
  Wire.setTimeOut(100);
  server = VOICE_SERVER_URL;
  while (server.endsWith("/")) server.remove(server.length() - 1);
  if (!server.startsWith("http://") || !strlen(VOICE_WIFI_SSID) || !strlen(VOICE_SERVER_KEY)) {
    Serial.println("Fill firmware/include/secrets.h using VoiceConfig.example.h first.");
    return;
  }
  audio = static_cast<uint8_t*>(heap_caps_malloc(256044, MALLOC_CAP_SPIRAM | MALLOC_CAP_8BIT));
  micReady = beginMicrophone();
  Serial.printf("Microphone: %s; PSRAM buffer: %s\n", micReady ? "ready" : "failed", audio ? "ready" : "failed");
  WiFi.mode(WIFI_STA);
  WiFi.setSleep(false);
  WiFi.begin(VOICE_WIFI_SSID, VOICE_WIFI_PASSWORD);
  Serial.println("Hold D1 button to talk, or send r in Serial Monitor for a 6-second recording.");
}

void loop() {
  const int command = Serial.available() ? Serial.read() : -1;
  if (command == 's') {
    Serial.printf("Microphone: %s; PSRAM buffer: %s; Wi-Fi status: %d\n",
      micReady ? "ready" : "failed", audio ? "ready" : "failed", int(WiFi.status()));
    if (WiFi.status() == WL_CONNECTED) {
      Serial.printf("Device IP: %s; backend: %s\n",
        WiFi.localIP().toString().c_str(), server.c_str());
    }
  }
  if (!audio || !micReady || !strlen(VOICE_WIFI_SSID) || !strlen(VOICE_SERVER_KEY)) {
    delay(100);
    return;
  }
  if (WiFi.status() != WL_CONNECTED) {
    if (millis() - lastWifi > 10000) {
      lastWifi = millis();
      WiFi.reconnect();
    }
    delay(20);
    return;
  }
  const bool pressed = digitalRead(D1) == LOW;
  if (pressed && !buttonHeld) {
    delay(30);
    if (digitalRead(D1) == LOW) {
      buttonHeld = true;
      askSleepAi(false);
    }
  }
  if (!pressed) buttonHeld = false;
  if (command == 'r') askSleepAi(true);
  if (millis() - lastSensor > 15000) {
    lastSensor = millis();
    sendEnvironment();
  }
  delay(10);
}
