#include "AudioCapture.h"
#include <driver/i2s.h>

bool beginMicrophone() {
  i2s_config_t config = {};
  config.mode = i2s_mode_t(I2S_MODE_MASTER | I2S_MODE_RX | I2S_MODE_PDM);
  config.sample_rate = 16000;
  config.bits_per_sample = I2S_BITS_PER_SAMPLE_16BIT;
  config.channel_format = I2S_CHANNEL_FMT_ONLY_LEFT;
  config.communication_format = I2S_COMM_FORMAT_STAND_I2S;
  config.intr_alloc_flags = ESP_INTR_FLAG_LEVEL1;
  config.dma_buf_count = 4;
  config.dma_buf_len = 512;
  if (i2s_driver_install(I2S_NUM_0, &config, 0, nullptr) != ESP_OK) return false;
  i2s_pin_config_t pins = {};
  pins.mck_io_num = I2S_PIN_NO_CHANGE;
  pins.bck_io_num = I2S_PIN_NO_CHANGE;
  pins.ws_io_num = 42;
  pins.data_out_num = I2S_PIN_NO_CHANGE;
  pins.data_in_num = 41;
  if (i2s_set_pin(I2S_NUM_0, &pins) != ESP_OK) {
    i2s_driver_uninstall(I2S_NUM_0);
    return false;
  }
  i2s_stop(I2S_NUM_0);
  return true;
}

static void put32(uint8_t* output, uint32_t value) {
  for (int i = 0; i < 4; ++i) output[i] = (value >> (8 * i)) & 0xff;
}

size_t recordWav(uint8_t* buffer, size_t capacity, bool serialMode) {
  if (capacity < 256044 || i2s_start(I2S_NUM_0) != ESP_OK) return 0;
  // Discard startup samples and stale DMA data.
  uint8_t warmup[1024];
  for (int i = 0; i < 4; ++i) {
    size_t discarded = 0;
    i2s_read(I2S_NUM_0, warmup, sizeof(warmup), &discarded, pdMS_TO_TICKS(150));
  }
  size_t length = 0;
  const uint32_t started = millis();
  const uint32_t duration = serialMode ? 6000 : 8000;
  while (millis() - started < duration && length < 256000 &&
         (serialMode || digitalRead(D1) == LOW)) {
    size_t count = 0;
    const size_t wanted = min(size_t(1024), size_t(256000 - length));
    if (i2s_read(I2S_NUM_0, buffer + 44 + length, wanted, &count,
                 pdMS_TO_TICKS(150)) != ESP_OK) {
      length = 0;
      break;
    }
    length += count;
    delay(1);
  }
  i2s_stop(I2S_NUM_0);
  if (length < 16000) return 0;
  memset(buffer, 0, 44);
  memcpy(buffer, "RIFF", 4); put32(buffer + 4, length + 36);
  memcpy(buffer + 8, "WAVEfmt ", 8); put32(buffer + 16, 16);
  buffer[20] = 1; buffer[22] = 1;
  put32(buffer + 24, 16000); put32(buffer + 28, 32000);
  buffer[32] = 2; buffer[34] = 16;
  memcpy(buffer + 36, "data", 4); put32(buffer + 40, length);
  return 44 + length;
}
