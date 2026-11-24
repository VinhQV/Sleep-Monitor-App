# Phần cứng và cấu hình PlatformIO

## Danh sách cho bản hiện tại

| Linh kiện | Số lượng | Vai trò |
|---|---:|---|
| ESP32-WROOM-32 DevKit, USB | 2 | Gateway cạnh giường và bộ đèn |
| Module SHT31 I²C, địa chỉ 0x44 | 1 | Nhiệt độ °C, độ ẩm tương đối % |
| Module BH1750 I²C, địa chỉ 0x23 | 1 | Độ rọi lux |
| Nút nhấn thường hở | 1–2 | Bắt đầu/kết thúc ngủ; mở setup khi khởi động |
| Dải/module LED đơn sắc 5V có giới hạn dòng | 1 | Đèn ngủ / đèn bình minh |
| Module MOSFET N-channel hỗ trợ tín hiệu 3.3V | 1 | Điều chỉnh LED bằng PWM |
| Nguồn 5V phù hợp tổng dòng LED, dây, breadboard | Theo thiết bị | Nguồn LED riêng, GND chung với bo đèn |

Chọn module MOSFET công bố chạy với tín hiệu 3.3V, kiểm tra dòng tải theo LED đã mua. Không dùng relay cơ để điều chỉnh độ sáng PWM. Bộ này điều khiển LED điện áp thấp; không đấu đèn 220V trực tiếp.

## Gateway: đấu nối

| ESP32 gateway | SHT31 | BH1750 | Khác |
|---|---|---|---|
| 3V3 | VIN/VCC phù hợp 3.3V | VCC | Hai module dùng mức I²C 3.3V |
| GND | GND | GND | Một chân nút nhấn |
| GPIO21 | SDA | SDA | Bus I²C chung |
| GPIO22 | SCL | SCL | Bus I²C chung |
| GPIO32 | — | — | Chân còn lại nút; INPUT_PULLUP |
| GPIO2 | — | — | LED trạng thái có sẵn nếu bo hỗ trợ |

Địa chỉ khác nhau nên dùng chung SDA/SCL. Không kéo SDA/SCL lên 5V. SHT31 để ADDR mặc định 0x44; BH1750 ADDR nối GND / mặc định 0x23. Đặt SHT31 xa nguồn nóng và bộ LED để số đo đại diện phòng.

## Lamp: đấu nối

| Kết nối | Đích |
|---|---|
| GPIO25 ESP32 | Tín hiệu IN module MOSFET tương thích 3.3V |
| GND ESP32 | GND module + cực âm nguồn LED |
| +5V nguồn LED | Cực dương LED |
| Cực âm LED | Ngõ tải đóng/cắt của module MOSFET |
| GPIO32 → nút → GND | Giữ khi reset để mở lại setup |

Nếu dùng MOSFET rời: source → GND; drain → LED−; LED+ → nguồn5V; GPIO25 → điện trở khoảng 100Ω → gate; gate → điện trở 10kΩ → GND. Chọn MOSFET có Rds(on) được đặc tả ở Vgs khoảng 2.5–3.3V và dòng định mức phù hợp. LED phải có giới hạn dòng. Không lấy dòng công suất LED từ chân GPIO/3V3 của ESP32.

PWM 5kHz, 10bit. Phản hồi đèn xác nhận **bo đã đặt PWM**, không xác nhận bằng cảm biến rằng LED thực sự phát sáng. BH1750 đặt ở gateway đo ánh sáng phòng để quan sát kết quả thực tế.

## Nạp và ghép cặp lần đầu

1. VS Code → PlatformIO → Open Project → thư mục `firmware`. `platformio.ini` có hai environment: `gateway`, `lamp`.
2. Nạp firmware cho từng bo. Chỉ bo gateway cần nạp thêm filesystem lần đầu như README. Không cắm cả hai rồi đoán cổng COM; dùng Device Manager / `pio device list` để xác định.
3. Mở Serial Monitor 115200 của mỗi bo, ghi lại **STA MAC**. Bo chưa có cấu hình sẽ mở Wi-Fi `Sleep-gateway-...` hoặc `Sleep-lamp-...`.
4. Kết nối vào Wi-Fi setup, mật khẩu mặc định `SleepSetup9`; mở `http://192.168.4.1`. Có thể đổi mật khẩu setup trong `include/DeviceConfig.h` trước khi build.
5. Nhập Wi-Fi 2.4GHz, mật khẩu, IPv4 máy chủ, mật khẩu MQTT. Nhập **MAC của bo còn lại**, không phải MAC của chính bo/AP.
6. Sinh một khóa ghép cặp 16byte và nhập cùng chuỗi 32 ký tự hex trên hai bo. Có thể chạy `node -e "console.log(require('crypto').randomBytes(16).toString('hex'))"` trên máy tính. Không dùng khóa minh họa chung cho nhiều nhóm.
7. Cố định kênh Wi-Fi của router, ví dụ kênh6. Nhập cùng kênh trên hai bo. ESP-NOW và Wi-Fi dùng chung radio; khi router đổi kênh, cần cập nhật cấu hình. Khi router mất, hai bo quay về kênh đã cấu hình, vẫn có thể điều khiển cục bộ.
8. Save and restart. Điện thoại quay lại Wi-Fi thường. Gateway kết nối NTP; nếu Internet không có, lấy thời gian từ `http://IP:8080/api/time` của máy chủ LAN.
9. App chuyển sang dữ liệu thật. Kiểm tra số đo, timestamp, trạng thái gateway/đèn. Thử đặt 10% và 70%; chỉ khi ACK từ bo đèn tới gateway rồi về server, app mới báo thành công.

Giữ nút GPIO32 khi reset/cấp nguồn để mở setup lại. Nút ngắn khi gateway chạy bình thường là bắt đầu/kết thúc ngủ. Nếu chưa đồng bộ giờ hoặc không lưu được sự kiện, firmware từ chối thao tác nút và báo Serial; không tạo timestamp giả.

## Hoạt động mất mạng

- Mất máy chủ nhưng gateway vẫn có điện/giờ đúng: cảm biến và lịch đèn vẫn chạy; LittleFS giữ dữ liệu chưa ACK.
- Mất Internet nhưng LAN còn: API thời gian và MQTT vẫn dùng được.
- Mất router: ESP-NOW trở về kênh cấu hình; trong lúc thử kết nối Wi-Fi lại có thể gián đoạn radio ngắn. Cần đo thực tế với router của nhóm.
- Mất điện toàn bộ: lịch chỉ tiếp tục khi gateway đồng bộ giờ lại. Chưa có RTC/nguồn dự phòng. Không coi đồng hồ từ lần bật trước còn chính xác sau khi mất điện.
- Bộ đệm tối đa 512 bản ghi, chừa 12 vị trí cho sự kiện; dung lượng LittleFS thực tế có thể đầy trước giới hạn này. Mỗi phút một mẫu khi MQTT mất, nên đây là bộ đệm hữu hạn, không hứa lưu nhiều ngày. Có log Serial khi đầy/lỗi.
- Không tự xóa filesystem khi mount lỗi. Chỉ nạp `uploadfs` để khởi tạo sau khi chắc chắn không còn dữ liệu cần giữ.

## Tài liệu chính thức đã đối chiếu

- [PlatformIO ESP32 Dev Module](https://docs.platformio.org/en/stable/boards/espressif32/esp32dev.html): định danh `esp32dev` và cấu hình build.
- [ESP-NOW ESP-IDF 4.4.7](https://docs.espressif.com/projects/esp-idf/en/v4.4.7/esp32/api-reference/network/esp_now.html): peer, kênh và khóa LMK. Bản firmware pin Arduino-ESP32 2.0.17 để khớp callback ESP-NOW của IDF4.x; không thay thư viện API3.x vào một phần dự án.
