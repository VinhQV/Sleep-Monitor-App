#include <Arduino.h>
#include <Wire.h>
#include <Adafruit_SHT31.h>
#include <math.h>

namespace {
constexpr uint8_t kSdaPin = D4;  // GPIO5
constexpr uint8_t kSclPin = D5;  // GPIO6
constexpr uint32_t kReadIntervalMs = 2000;
Adafruit_SHT31 sensor(&Wire);
bool sensorReady = false;
uint32_t lastReadMs = 0;

bool connectSensor() {
  // SHT30 supports 0x44 (ADDR low) and 0x45 (ADDR high).
  for (uint8_t address : {uint8_t(0x44), uint8_t(0x45)}) {
    Wire.beginTransmission(address);
    if (Wire.endTransmission() != 0) {
      continue;
    }
    if (sensor.begin(address)) {
      sensor.heater(false);
      Serial.printf("SHT30 connected at 0x%02X\n", address);
      return true;
    }
  }
  Serial.println("SHT30 not found. Check 3V3, GND, SDA=D4, SCL=D5.");
  return false;
}
}  // namespace

void setup() {
  Serial.begin(115200);
  const uint32_t serialStartedMs = millis();
  while (!Serial && millis() - serialStartedMs < 3000) {
    delay(10);
  }
  Serial.println("\nXIAO ESP32S3 Sense + SHT30");
  Wire.begin(kSdaPin, kSclPin, 100000);
  Wire.setTimeOut(100);
  sensorReady = connectSensor();
}

void loop() {
  if (millis() - lastReadMs < kReadIntervalMs) {
    delay(10);
    return;
  }
  lastReadMs = millis();
  if (!sensorReady) {
    sensorReady = connectSensor();
    if (!sensorReady) {
      return;
    }
  }

  float temperature;
  float humidity;
  // Read both values from the same measurement. The library checks the CRC.
  if (!sensor.readBoth(&temperature, &humidity) ||
      !isfinite(temperature) || !isfinite(humidity) ||
      temperature < -40 || temperature > 125 ||
      humidity < 0 || humidity > 100) {
    Serial.println("SHT30 read failed. Retrying connection on next cycle.");
    sensorReady = false;
    return;
  }
  Serial.printf("{\"temperature\":%.2f,\"humidity\":%.2f}\n",
                temperature, humidity);
}
