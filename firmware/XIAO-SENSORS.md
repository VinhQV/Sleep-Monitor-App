# XIAO ESP32S3 Sense — công tắc nguồn pin, SHT30 và ánh sáng ADC

Mở thư mục firmware trong VS Code + PlatformIO, chọn environment
`xiao_sensors` (đã là environment mặc định). Bản này tự đọc cảm biến và
gửi heartbeat khi được cấp nguồn. D1 không còn dùng để bật/tắt bằng phần mềm.

## Sơ đồ chân

Rút USB và tháo pin khỏi khay trước khi đấu/hàn dây. Dùng nhãn chân in trên
XIAO và module. Tháo toàn bộ dây D1–GND cũ khỏi công tắc trước khi đổi sang
ngắt nguồn pin; không được để dương pin nối với GND hoặc GPIO.

| Thiết bị | Chân thiết bị | Chân XIAO |
| --- | --- | --- |
| SHT30 | VCC | 3V3 |
| SHT30 | GND | GND |
| SHT30 | SDA | D4 / GPIO5 |
| SHT30 | SCL | D5 / GPIO6 |
| Module ánh sáng trong ảnh | VCC | 3V3 |
| Module ánh sáng | GND | GND |
| Module ánh sáng | AO | D0 / GPIO1 / ADC1 |
| Khay pin Li-ion 1 cell 3,7 V (đầy 4,2 V) | Âm (−) | BAT− ở mặt dưới XIAO |
| Khay pin | Dương (+) | Chân giữa COM của công tắc nguồn |
| Công tắc nguồn 3 chân | Một chân ngoài | BAT+ ở mặt dưới XIAO |
| Công tắc nguồn | Chân ngoài còn lại | Để trống, không nối GND |
| XIAO | D1 / GPIO2 | Để trống |

Đầu DO của module ánh sáng để trống. Cấp module bằng 3V3 để AO phù hợp
với điện áp GPIO của XIAO. Nguồn 3V3 và GND dùng chung cho hai cảm biến.
Nếu module SHT30 có ADDR, nối GND để dùng địa chỉ 0x44.

D0 dùng ADC1, phù hợp khi Wi-Fi đang chạy. D4/D5 giữ bus I2C.
D1, D3, D6–D10 còn trống để mở rộng. GPIO3/D2,
USB GPIO19/20 và chân camera/micro được dành lại.

## Bật và tắt

Công tắc nguồn phải có định mức phù hợp dòng tải của XIAO và cảm biến.
Đo thông mạch để xác định chân COM và vị trí ON/OFF trước khi lắp pin.

```text
Pin (+) ---- công tắc ---- BAT+ XIAO
Pin (−) ----------------- BAT− XIAO
```

- Khi chỉ chạy bằng pin: ON cấp nguồn, ESP32 tự khởi động, kết nối Wi-Fi,
  gửi trạng thái ON trước số đo đầu tiên; sau đó cập nhật mỗi 5 giây.
- OFF cắt nguồn pin khỏi XIAO và các cảm biến lấy nguồn từ 3V3 của XIAO.
  Không có bản tin OFF khi nguồn bị cắt đột ngột. Backend xác định ngoại tuyến
  sau 15 giây không có heartbeat; app kiểm tra mỗi 5 giây, nên thường hiển thị
  “Mất kết nối thiết bị” trong khoảng 15–20 giây kể từ tín hiệu cuối.
- Bật lại tự kết nối và hiển thị số đo mới. Không cần nối D1 xuống GND.
- Cắm USB-C vẫn cấp điện cho board dù công tắc pin OFF. Vì vậy phải rút USB
  và các nguồn phụ khi thử ngắt nguồn hoàn toàn.
- Công tắc ON cho phép sạc pin qua USB-C; OFF ngắt pin khỏi bộ sạc nên pin
  không sạc được. Pin không nối vào chân 3V3 hoặc 5V.
- Mất Wi-Fi hoặc máy chủ không truy cập được cũng khiến app báo ngoại tuyến.
  Thiết bị tự thử kết nối lại khi mạng trở về.

Firmware không đo phần trăm pin và không thay thế mạch bảo vệ pin.

## Đo ánh sáng

ADC 12-bit cho kết quả 0–4095, lấy trung bình 16 mẫu.
App hiện giá trị ADC, điện áp AO theo mV và biểu đồ ADC.
Giá trị này phụ thuộc module và vị trí đặt cảm biến. Hãy che rồi chiếu sáng
để quan sát chiều thay đổi. Muốn đo lux cần hiệu chuẩn hoặc dùng cảm biến lux.
Ngưỡng lux của các cấu hình cũ được lưu riêng và không áp dụng lên ADC.

## Cấu hình và nạp

`include/secrets.h` trên máy đã có cấu hình Wi-Fi và backend riêng.
Nếu đổi IP máy tính, cập nhật VOICE_SERVER_URL trước khi nạp.
Để tạo cấu hình trên máy khác, sao chép `include/VoiceConfig.example.h`
thành `include/secrets.h` rồi điền thông tin kết nối.

Terminal tại thư mục firmware:

```powershell
pio run -e xiao_sensors
pio run -e xiao_sensors -t upload
pio device monitor -e xiao_sensors --baud 115200 --dtr 0 --rts 0
```

Mở `Start-Sleep-AI.cmd` để chạy backend. App chọn nguồn thiết bị thật.
Máy ảo dùng http://10.0.2.2:8080. Board dùng IP LAN của máy tính,
cùng mạng Wi-Fi 2.4 GHz.

## Demo

1. Nạp bản `xiao_sensors` mới qua USB. Xác nhận dữ liệu gửi được kể cả khi D1 hở.
2. Rút USB, đấu công tắc nối tiếp BAT+ như trên, kiểm tra cực tính rồi lắp pin.
3. Bật công tắc, vào Môi trường trên app.
4. Xem nhiệt độ, độ ẩm, ADC và mV cập nhật; che/chiếu sáng để xem ADC thay đổi.
5. Tắt công tắc khi USB đã rút, đợi khoảng 15–20 giây, kiểm tra app báo mất
   kết nối và ô số đo chuyển thành gạch ngang.
6. Bật lại, kiểm tra số đo mới xuất hiện sau khi kết nối Wi-Fi/máy chủ.

Thông báo Serial khi gửi thành công là `Sensors synced` và
`Heartbeat synced (powered ON)`. Dòng `Power-switch firmware` xác nhận bản
firmware mới. Gõ `s` để xem trạng thái Wi-Fi. Không còn log `OFF status synced`.

## Tài liệu chân

https://wiki.seeedstudio.com/xiao_esp32s3_getting_started/
