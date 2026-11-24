# Sleep Monitor — Nhóm 9, Đồ án 15

**Android 4.2.0 · XIAO ESP32S3 Sense · SHT30 · ánh sáng ADC.** App ghi giờ đi ngủ/thức dậy, tính thời lượng, ghi cảm nhận, xem lịch sử từng ngày và thống kê tuần/tháng. Giao diện sáng/tối, Liquid Glass, nhắc nhở cục bộ và Sleep AI. Firmware C++/Arduino trong PlatformIO, VS Code. Không dùng radar.

## Chạy trên máy hiện tại

- `Start-Android.cmd`: mở app bằng máy ảo 2 GB RAM, 4 lõi CPU, cùng máy chủ cần thiết; không mở Android Studio hay model AI local.
- `Start-Remote.cmd`: giữ máy tính làm server, cho app/XIAO dùng qua Internet khác mạng. Mở tệp riêng `backend/data/Ket-noi-tu-xa.html`, lấy mã cho **Cài đặt → Kết nối từ xa**. Máy tính cần bật, online, không Sleep.
- Build APK trong `android` bằng `.\gradlew.bat :app:assembleDebug`. APK nằm tại `android/app/build/outputs/apk/debug/app-debug.apk` và hỗ trợ Android 8.0 trở lên. Cập nhật bằng `adb install -r` để giữ dữ liệu.

Kết nối từ xa đang dùng Cloudflare Quick Tunnel: địa chỉ tạm có thể đổi khi tạo lại; phải cập nhật mã app và URL trên board. Chưa phải domain cố định của dịch vụ thương mại. Xem [hướng dẫn remote](docs/REMOTE-ACCESS.md).

## Thành phần

| Thành phần | Vị trí |
|---|---|
| Kotlin + Jetpack Compose | `android` |
| PlatformIO XIAO, environment mặc định `xiao_sensors` | `firmware` |
| Node.js 24+, REST/SQLite, AI và MQTT tương thích bo cũ | `backend` |
| APK sau khi build | `android/app/build/outputs/apk/debug` |
| Remote HTTPS và ghép thiết bị | [REMOTE-ACCESS.md](docs/REMOTE-ACCESS.md) |
| Nhắc ngủ, lịch và đổi Wi-Fi không nạp lại | [ANDROID-REMINDERS-WIFI.md](docs/ANDROID-REMINDERS-WIFI.md) |
| Gemini và cá nhân hóa | [AI.md](docs/AI.md) |
| Yêu cầu môn học | [REQUIREMENTS.md](docs/REQUIREMENTS.md) |

## Thiết bị hiện tại

SHT30 dùng SDA→D4, SCL→D5; cảm biến ánh sáng AO→D0; cảm biến lấy 3V3/GND. Nút BOOT giữ ba giây mở Wi-Fi thiết lập. Công tắc nguồn nằm trên đường pin; D1 không dùng làm công tắc phần mềm. `xiao_sensors` không chạy microphone/chat AI trên board.

XIAO gửi nhiệt độ, độ ẩm, ánh sáng ADC/mV qua HTTP LAN hoặc HTTPS Internet. Không đổi ADC thành lux khi chưa hiệu chuẩn. Khi board mất nguồn/mạng, server hết hạn heartbeat và app báo mất kết nối; có thể giữ mẫu cũ kèm thời điểm. Không suy ra SpO₂, nhịp tim, tiếng ngáy, CO₂ hay giai đoạn ngủ từ cảm biến phòng.

```powershell
# Trong firmware, sau khi tạo include/secrets.h từ mẫu cấu hình
pio run -e xiao_sensors
pio run -e xiao_sensors -t upload --upload-port COMx
pio device monitor -p COMx -b 115200
```

Thay COMx bằng cổng thực tế. Cấu hình trong NVS được ưu tiên; sửa secrets.h không tự ghi đè Wi-Fi đã lưu. Dùng portal BOOT hoặc `tools/Configure-Remote-Device.py` để cập nhật. Binary XIAO chứa cấu hình riêng nên không đưa vào ZIP. Hai environment `gateway`/`lamp` và binary tương ứng trong release là hệ thống ESP32-WROOM cũ, **không nạp cho XIAO**; hướng dẫn cũ trong [HARDWARE.md](docs/HARDWARE.md).

## Nhật ký và trợ lý

Bốn tab **Tổng quan · Ghi ngủ · Lịch sử · Thống kê**, nút **Sleep AI** ở giữa. Môi trường mở từ thẻ Phòng ngủ hoặc Cài đặt. SQLite giữ nhật ký khi offline; bộ dữ liệu trải nghiệm tách khỏi dữ liệu thật. Giờ bắt đầu/kết thúc là do người dùng ghi, không phải đo sinh lý tự động.

Gemini chạy trên cloud; khóa nhà cung cấp chỉ ở máy chủ. Bản 4.2 bỏ nhãn nhà cung cấp/sẵn sàng và cảnh báo cố định trong chat, vẫn hiển thị lỗi kết nối cần xử lý. **Cài đặt → Trợ lý giấc ngủ → Cá nhân hóa từ nhật ký** cho phép dùng tổng hợp bảy ngày và số đo phòng; không tự gửi ghi chú riêng. Tắt lựa chọn ngừng gửi context mới, không xóa dữ liệu đã gửi trong hội thoại cũ.

## Máy chủ và kiểm thử

Trong `backend`, chạy `npm ci` một lần rồi dùng launcher. Tùy chọn môi trường ở `.env.example`. Dùng LAN khi cần: emulator `http://10.0.2.2:8080`, điện thoại/board dùng IP máy tính truy cập được. Không mở cổng HTTP/MQTT trực tiếp ra Internet; remote gateway có khóa app/thiết bị riêng và HTTPS.

```powershell
# backend
npm test

# android: đặt JAVA_HOME và ANDROID_HOME đúng máy
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon --max-workers=1 '-Pkotlin.compiler.execution.strategy=in-process'

# firmware
pio run -e xiao_sensors
```

Kiểm thử điều hướng trên emulator đang giữ nhật ký: build `:app:assembleDebugAndroidTest`, cài app/test APK bằng `adb install -r`, rồi chạy `adb shell am instrument -w -e class com.example.sleepmonitor.TabNavigationTest com.example.sleepmonitor.test/androidx.test.runner.AndroidJUnitRunner`. Không dùng `connectedDebugAndroidTest` trên máy chứa dữ liệu cần giữ vì tác vụ có thể gỡ app sau kiểm thử.

Phạm vi triển khai vẫn **một người/một phòng**, chưa có tài khoản đa người, domain cố định hay OTA. [Kết quả kiểm chứng](docs/VERIFICATION.md). ZIP loại dữ liệu riêng, secrets, cache/build và cấu hình máy; APK là debug signing để thử và bảo vệ đồ án.
