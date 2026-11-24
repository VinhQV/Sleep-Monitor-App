# XIAO ESP32S3 Sense + SHT30

Mở thư mục firmware bằng VS Code + PlatformIO. Chọn environment
`xiao_sht30` để nạp chương trình đọc cảm biến qua USB Serial.

## Đấu dây

Ngắt nguồn trước khi đấu dây. Dùng module SHT30 I2C có chân nguồn và tín hiệu.

| SHT30 | XIAO ESP32S3 Sense |
| --- | --- |
| VCC / VIN | 3V3 |
| GND | GND |
| SDA | D4 / GPIO5 |
| SCL | D5 / GPIO6 |

Module cần điện trở kéo lên SDA/SCL về 3V3. Nếu module chưa có sẵn,
thêm mỗi đường một điện trở 4,7 kΩ. Chân ADDR nếu có nối GND để chọn 0x44,
hoặc 3V3 để chọn 0x45. Code tự thử cả hai địa chỉ.

## Build, nạp và xem số đo

Trong terminal tại thư mục firmware:

```powershell
pio run -e xiao_sht30
pio run -e xiao_sht30 -t upload
pio device monitor -e xiao_sht30
```

Serial Monitor dùng 115200 baud. Mỗi 2 giây xuất một số đo:

```json
{"temperature":26.35,"humidity":61.20}
```

Nhiệt độ tính bằng °C, độ ẩm tương đối tính bằng %RH.
Khi cảm biến mất kết nối hoặc đọc lỗi, chương trình báo lỗi và tự thử lại.

Nếu không nạp được, giữ BOOT, bấm rồi thả RESET, sau đó thả BOOT và nạp lại.
Cổng USB có thể đổi sau khi nạp, hãy chọn lại cổng trong PlatformIO.

Chương trình này kiểm tra SHT30 bằng Serial. Để số đo hiện trên app,
cần cấu hình Wi-Fi, MQTT và tích hợp vào gateway của hệ thống.
Firmware gateway hiện tại dùng ESP32 DevKit, SHT3x và BH1750.

## Tài liệu

- Seeed pinout: https://wiki.seeedstudio.com/xiao_esp32s3_getting_started/
- Driver SHT3x: https://github.com/adafruit/Adafruit_SHT31
