# Android — Nhắc nhở và thiết lập Wi-Fi

Chạy `Start-Android.cmd`. Chỉ mở máy chủ cần cho Android/IoT và một máy giả lập Pixel_3 với 2048 MB RAM, 4 lõi CPU; không mở Android Studio, trình duyệt hoặc model AI local. Không xóa dữ liệu máy giả lập.

## Nhắc nhở hằng ngày

Android → Cài đặt → Nhắc nhở giấc ngủ → bật **Nhắc tôi ghi nhật ký** và cấp quyền thông báo. Mặc định 22:30 và 07:30; thay đổi theo lịch cá nhân. Nút **Thử thông báo** kiểm tra quyền và chạm thông báo để mở app.

- Buổi tối: gợi ý bắt đầu ghi ngủ nếu chưa có phiên đang ghi và chưa vừa thức dậy trong hai giờ qua.
- Buổi sáng: gợi ý đánh giá phiên kết thúc trong 24 giờ chưa có điểm. Nếu chưa có nhật ký gần đây, gợi ý bổ sung giờ thủ công.
- Khi đang ghi ngủ: tạm ngưng cả hai loại nhắc. Mỗi loại tối đa một lần/ngày. Thông báo không phát âm thanh.
- Theo thói quen: sau ít nhất ba phiên từ ba giờ trong 14 ngày, nếu giờ ngủ tương đối ổn định, lấy trung vị và nhắc sớm 15 phút. Nếu chưa đủ dữ liệu hoặc lịch thất thường, dùng giờ đã chọn.
- Android dùng lịch nhắc không chính xác đến từng phút để tránh yêu cầu quyền báo thức đặc biệt. Chế độ tiết kiệm pin có thể làm giao thông báo trễ. Khởi động lại điện thoại/thay đổi múi giờ sẽ đặt lại lịch.

SHT30 và cảm biến ánh sáng đo môi trường, không xác định chắc chắn người dùng đã ngủ. Bản này không tự khẳng định đã ngủ, không tự tạo phiên từ ánh sáng và không dùng microphone để theo dõi.

## Tự đổi Wi-Fi XIAO ESP32S3 Sense

Firmware `xiao_sensors` đã thêm trang thiết lập, cần **nạp bản mới một lần**. Đã nạp thành công qua COM17 ngày 22/11/2026. Sau đó đổi mạng không phải sửa mã/nạp lại.

1. Bật board, chờ chạy rồi giữ nút **BOOT 3 giây**. Không giữ BOOT trong lúc bật nguồn vì đó là thao tác vào bootloader.
2. Android → Cài đặt → Thiết lập Wi-Fi thiết bị → Mở cài đặt Wi-Fi. iPhone mở Cài đặt Wi-Fi của hệ thống.
3. Vào `Sleep-Setup-XXXXXX`; mật khẩu `Sleep-XXXXXX`, sáu ký tự cuối giống tên mạng, phân biệt chữ hoa. Chấp nhận giữ kết nối dù mạng này không có Internet.
4. Mở `http://192.168.4.1` hoặc nút Mở trang thiết lập. Nhập tên Wi-Fi 2.4 GHz, mật khẩu, địa chỉ HTTPS từ tệp kết nối riêng, hoặc `http://IP-MAY-TINH:8080` khi dùng LAN. Khóa máy chủ để trống nếu giữ nguyên.
5. Chờ tối đa 25 giây. Chỉ lưu cấu hình khi Wi-Fi kết nối thành công. Sai mật khẩu sẽ khôi phục cấu hình cũ. Kết nối điện thoại lại Wi-Fi nhà và xem dữ liệu trong app.

Trang thiết lập tự đóng sau 10 phút hoặc khoảng 8 giây sau khi lưu thành công. Khi khởi động mà không kết nối được Wi-Fi trong khoảng một phút, board mở trang thiết lập tự động một lần. Khi dùng HTTP LAN, máy chủ phải truy cập được trong mạng đó. Bản 4.2 hỗ trợ HTTPS từ xa: board và máy tính không cần cùng mạng; xem [REMOTE-ACCESS.md](REMOTE-ACCESS.md). Kết nối Wi-Fi thành công chưa bảo đảm máy chủ đang truy cập được.

Giữ chân hiện có: SHT30 SDA→D4, SCL→D5; ánh sáng AO→D0; cảm biến dùng 3V3/GND. Nút BOOT tích hợp mở thiết lập, không chiếm thêm chân ngoài. Công tắc nguồn vật lý vẫn nối tiếp nguồn pin; D1 không dùng làm công tắc phần mềm.

Nạp trong terminal PlatformIO ở thư mục `firmware`:

```powershell
pio run -e xiao_sensors -t upload --upload-port COMx
pio device monitor -p COMx -b 115200
```

Thay COMx bằng cổng board thực tế. Binary build cục bộ có thể chứa cấu hình riêng nên không được đưa vào ZIP phát hành; bộ mã nguồn có mẫu cấu hình. Wi-Fi Vinh đã kết nối và số đo SHT30/ADC đã tới máy chủ; luồng đổi sang mạng khác bằng portal chưa được thử.

## Kết quả kiểm chứng ngày 22/11/2026

- Android: build APK thành công; 14 unit test và 4 kiểm tra trên emulator đạt, gồm phát thông báo im lặng và mở app từ thông báo.
- XIAO: PlatformIO build `xiao_sensors` thành công. Đã nạp COM17, xác nhận Wi-Fi và heartbeat/số đo thật; chưa thử đổi sang Wi-Fi khác trong portal.

- Sau khi gỡ phần web: 21 kiểm tra máy chủ đạt. Endpoint trang web và Web Push trả 404; nhật ký được giữ nguyên.
- Giả lập Android 16 yêu cầu tối thiểu 2 GB RAM. Launcher dùng 720×1440, tắt Vulkan, camera, âm thanh và không mở IDE/model AI.
- Đã xem số đo thật trên màn hình Phòng ngủ Android sau khi nạp. Bộ giả lập trên Windows dùng khoảng 3,6 GB working set trong phiên kiểm tra, gồm RAM Android và phần dựng hình; máy chủ khoảng 57 MB. Mức 2 GB là RAM cấp cho Android, không phải tổng RAM tiến trình giả lập.
