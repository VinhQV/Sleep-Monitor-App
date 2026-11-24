# Nói chuyện với Sleep AI bằng XIAO ESP32S3 Sense

Environment PlatformIO: `xiao_sleep_ai`. Lắp bo mở rộng Sense có micro vào
XIAO. Micro PDM tích hợp dùng GPIO42 (clock) và GPIO41 (data).
Chương trình ghi WAV PCM mono 16 kHz, 16-bit, dài 0,5–8 giây.

## Đấu dây

| Linh kiện | XIAO |
| --- | --- |
| SHT30 VCC | 3V3 |
| SHT30 GND | GND |
| SHT30 SDA | D4 / GPIO5 |
| SHT30 SCL | D5 / GPIO6 |
| Nút nhấn thường hở, chân 1 | D1 / GPIO2 |
| Nút nhấn thường hở, chân 2 | GND |

Ngắt nguồn khi đấu dây. Nút dùng điện trở kéo lên nội của ESP32.
Micro đã nằm trên bo Sense. Camera và thẻ nhớ chưa được dùng trong firmware này.

## Cấu hình và chạy

1. Mở thư mục firmware trong VS Code + PlatformIO.
2. Điền `include/secrets.h` theo `include/VoiceConfig.example.h`.
   Máy đang dùng đã được cấu hình Wi-Fi theo thông tin bạn cung cấp.
   Máy khác cần nhập lại SSID, mật khẩu, IP máy chủ và API_KEY của backend.
   Gemini API key chỉ lưu ở backend.
3. Board và máy tính chạy backend cần cùng mạng Wi-Fi. ESP32 dùng Wi-Fi 2.4 GHz.
   Máy tính phải cho phép Node.js nhận kết nối trên mạng riêng qua cổng backend.
4. Chạy `Start-Sleep-AI.cmd` ở thư mục SleepMonitor. Backend cần
   `HOST=0.0.0.0` trong `backend/.env`. Sleep AI phải báo Gemini sẵn sàng.
5. Cắm board bằng cáp USB có truyền dữ liệu và chạy các lệnh dưới đây.

```powershell
pio run -e xiao_sleep_ai -t upload
pio device monitor -e xiao_sleep_ai
```

Nếu khó vào chế độ nạp, giữ BOOT, bấm và thả RESET, rồi thả BOOT và nạp lại.

## Sử dụng

- Giữ nút D1 và nói một câu hỏi về giấc ngủ. Đèn sáng khi thu.
  Thả nút để gửi. Sau tối đa 8 giây, bản thu tự kết thúc.
- Có thể gõ `r` vào Serial Monitor để ghi 6 giây khi chưa có nút ngoài.
- Mở Sleep AI trong bản app mới. Màn hình tự nhận câu hỏi đã nhận diện
  và câu trả lời trong khoảng 3 giây sau khi máy chủ xử lý xong.
- Mỗi yêu cầu giọng nói là một câu hỏi độc lập. Các câu hỏi tiếp theo qua
  hộp chat của app có thể dùng các tin nhắn vừa nhận làm ngữ cảnh.
- SHT30 gửi nhiệt độ và độ ẩm mỗi 15 giây. Để xem trên app,
  chọn nguồn Thiết bị thật trong Cài đặt, cùng địa chỉ backend và API_KEY.
  Chưa có BH1750 nên giá trị ánh sáng để trống.

## Dữ liệu và giới hạn

Thao tác nhấn nút hoặc gõ r sẽ gửi bản thu tới Google Gemini qua backend.
Backend giữ bản thu trong bộ nhớ khi xử lý. Hộp thư lưu tối đa 20 lượt hỏi/đáp
để đồng bộ vào app. Firmware kết nối HTTP với backend trong mạng riêng.
Gemini có giới hạn lượt dùng theo tài khoản, bản thu và câu trả lời có thể
bị từ chối khi hết quota. Mỗi phút backend nhận tối đa 6 yêu cầu giọng nói
và xử lý từng bản thu. Khi lỗi, xem thông báo trên Serial Monitor.

Thiết bị hiện trả lời bằng chữ trên app và Serial Monitor.
Muốn nghe câu trả lời qua loa cần mạch I2S, loa và phần phát giọng nói.

Sau khi đổi mạng hoặc IP máy tính, cập nhật secrets.h và nạp lại.
Giữ file này và firmware.bin đã cấu hình riêng vì chứa thông tin truy cập.
File secrets.h và bản firmware có cấu hình riêng được loại khỏi gói mã nguồn chia sẻ.

## Tham khảo

- https://wiki.seeedstudio.com/xiao_esp32s3_sense_mic/
- https://ai.google.dev/gemini-api/docs/audio
