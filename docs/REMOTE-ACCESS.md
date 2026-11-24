# Kết nối từ xa — Android 4.2.0 và XIAO

Máy tính của bạn chạy máy chủ. Android và XIAO gọi cùng địa chỉ HTTPS qua Internet; không cần cùng Wi-Fi với nhau hoặc với máy tính. Máy tính phải đang bật, có Internet và không vào Sleep. Gemini vẫn chạy trên cloud, không nạp model AI vào RAM máy tính.

## Mở máy chủ

1. Chạy `Start-Remote.cmd` ở thư mục gốc dự án. Bộ khởi chạy mở backend, gateway và Cloudflare Tunnel ở nền. Chạy lại sẽ dùng tunnel đang hoạt động, không mở thêm bản sao.
2. Mở tệp riêng `backend/data/Ket-noi-tu-xa.html` trên máy tính. Tệp chứa mã ghép điện thoại và thông số dành riêng cho XIAO; không đưa vào Git hoặc gửi công khai.
3. Build Android bằng `.\gradlew.bat :app:assembleDebug`, sau đó cài `android/app/build/outputs/apk/debug/app-debug.apk` trên điện thoại. Vào **Cài đặt → Kết nối từ xa**, dán mã điện thoại, bấm **Kết nối**. Điện thoại có thể dùng 4G/5G hoặc Wi-Fi khác.

Máy ảo đã có launcher `Start-Android.cmd`; khi tồn tại cấu hình remote, launcher cũng khởi chạy máy chủ từ xa. Cài APK bằng `adb install -r` để giữ dữ liệu.

## XIAO ở nơi khác

Firmware dùng environment PlatformIO `xiao_sensors`. Sau khi nạp firmware hỗ trợ HTTPS một lần, có thể đổi mạng và địa chỉ máy chủ mà không nạp lại:

1. Khi board đang chạy, giữ **BOOT 3 giây**.
2. Kết nối Wi-Fi `Sleep-Setup-XXXXXX`, mật khẩu `Sleep-XXXXXX` với cùng sáu ký tự cuối. Chấp nhận kết nối không Internet.
3. Mở `http://192.168.4.1`. Nhập Wi-Fi **2.4 GHz tại nơi đặt board**, mật khẩu, địa chỉ HTTPS và **khóa thiết bị** từ tệp riêng phía trên.
4. Lưu; board khởi động lại sau khi cấu hình thành công. Kết nối điện thoại trở lại Internet và mở **Phòng ngủ** để xem số đo.

Nếu đang cắm USB, có thể chạy Python của PlatformIO:

```powershell
python tools/Configure-Remote-Device.py --port COM17 --wifi-from-secrets
```

Python cần `pyserial`; thay COM17 bằng cổng thực tế. Lệnh lấy Wi-Fi từ `firmware/include/secrets.h` và địa chỉ/khóa thiết bị từ cấu hình riêng, không in mật khẩu. Bỏ `--wifi-from-secrets` để giữ Wi-Fi đã lưu trên board.

SHT30 giữ SDA→D4, SCL→D5; ánh sáng AO→D0, nguồn cảm biến 3V3/GND. Chưa dùng microphone trong `xiao_sensors`. Mẫu ánh sáng là ADC/mV, không gắn nhãn lux khi chưa hiệu chuẩn. App giữ số đo cuối cùng nhưng báo mất kết nối khi heartbeat hết hạn; tắt nguồn không thể gửi một thông báo cuối ngay tức thì.

## Địa chỉ miễn phí có thể thay đổi

Đang dùng **Cloudflare Quick Tunnel**, không cần tài khoản/domain. Đây là địa chỉ tạm để thử và trình diễn, không có bảo đảm uptime. Khi tạo tunnel mới, hostname đổi. Mất mạng kéo dài cũng có thể khiến tunnel cũ hết hiệu lực. Launcher phát hiện thông báo `Tunnel not found` để tạo lại, rồi ghi mã kết nối mới vào tệp riêng. Cần cập nhật mã trong app và địa chỉ máy chủ trên XIAO. [Giới hạn chính thức của Quick Tunnel](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/).

Để dùng lâu dài với một địa chỉ cố định, cần chuyển sang named tunnel gắn domain hoặc nơi chạy backend có URL cố định. Phiên bản này chưa cấu hình lựa chọn đó.

## Xác thực và dữ liệu

- Internet chỉ đi qua gateway localhost `8081`, được tunnel công bố bằng HTTPS. Không mở cổng router 8080/1883 ra Internet.
- Khóa điện thoại và khóa thiết bị được tạo ngẫu nhiên, tách biệt. Khóa thiết bị không đọc được nhật ký; khóa LAN mặc định không đăng nhập được gateway public.
- XIAO xác minh chứng chỉ máy chủ bằng CA và đồng bộ giờ qua NTP; không dùng `setInsecure`. Gateway giới hạn đường dẫn, dung lượng và tần suất yêu cầu.
- Khi đổi URL tới cùng máy chủ đã xác thực bằng `serverId`, app giữ hội thoại cũ. Nhật ký tiếp tục nằm trong SQLite và đồng bộ qua API.
- **Cài đặt → Trợ lý giấc ngủ → Cá nhân hóa từ nhật ký** cho phép Gemini dùng tổng hợp bảy ngày và môi trường phòng. Ghi chú riêng không được tự đính kèm. Xem [AI.md](AI.md).
- Phạm vi vẫn là **một người/một phòng**. Mã kết nối điện thoại trao quyền truy cập phòng đó; chưa có tài khoản, phân quyền nhiều người hoặc thu hồi khóa riêng cho từng điện thoại.
- ZIP không chứa `backend/data`, `.env`, `secrets.h`, bộ nhớ Android hay binary XIAO chứa cấu hình riêng.
