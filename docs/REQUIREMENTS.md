# Đối chiếu yêu cầu và kịch bản bảo vệ

Nguồn yêu cầu bắt buộc: **Danh sach do an PTUD 1.pdf, trang12, Đồ án15: Ứng dụng theo dõi giấc ngủ cá nhân**. Bảng nhóm trong ảnh người dùng gửi ghi nhóm9. Các tính năng dưới đây tách rõ yêu cầu gốc và phần phát triển.

| Yêu cầu của thầy | Đã triển khai ở đâu | Cách chứng minh |
|---|---|---|
| Ghi giờ đi ngủ và thức dậy | Tổng quan Start/Wake; Ghi ngủ chọn cả ngày và giờ | Ghi một phiên 23:00 hôm trước → 06:30 hôm sau |
| Tính tổng thời gian ngủ | Home, form, chi tiết, lịch sử | Hiển thị 7h30m, tính bằng timestamp qua ngày |
| Ghi cảm nhận/chất lượng | 5 mức, ghi chú, ngủ gián đoạn; dialog khi thức dậy | Chọn Tốt, thêm ghi chú, mở lại |
| Lịch sử giấc ngủ theo ngày | Lịch sử có tìm ngày/ghi chú, lọc cảm nhận; chi tiết/sửa/xóa | Khởi động lại app vẫn còn dữ liệu |
| Trung bình theo tuần/tháng | Thống kê Tuần này/Tháng này | Cộng các phiên theo ngày thức dậy rồi lấy trung bình các ngày có bản ghi |

Thống kê không tính ngày chưa nhập là 0 giờ. Biểu đồ chỉ hiển thị ngày có dữ liệu; mục tiêu 8h là mốc giao diện, không phải đơn thuốc cá nhân. Phiên đang ngủ hoặc đã xóa không cộng vào trung bình. Phiên hoàn tất dài tối đa 48 giờ nhằm bắt lỗi nhập ngày; đây là giới hạn nhập liệu.

## Phần phát triển app + IoT

| Tính năng | Cách làm |
|---|---|
| Phòng ngủ gần realtime | SHT31 + BH1750, MQTT → SQLite server → REST app |
| Đánh giá môi trường | Ngưỡng chỉnh được, báo thiếu số đo và dữ liệu cũ |
| Lịch sử môi trường theo phiên | Truy vấn đúng khoảng bắt đầu/kết thúc; biểu đồ và timeline |
| Hai thiết bị phối hợp | Gateway gửi lệnh ESP-NOW có mã hóa sang lamp, lamp ACK sau khi áp PWM |
| Giảm sáng khi bắt đầu ngủ | Từ độ sáng đang yêu cầu xuống0 trong thời gian cấu hình |
| Bình minh chủ động | Tăng0→100 trước giờ thức dậy, chạy trên ESP32 sau đồng bộ giờ |
| Nút cạnh giường | Bắt đầu/thức dậy không cần chạm điện thoại |
| Đồng bộ sau mất mạng | SQLite điện thoại, LittleFS gateway, ID chống trùng, version phát hiện xung đột |
| Thiết lập thiết bị | Wi-Fi setup nhập Wi-Fi, IP server, MAC, khóa ghép cặp; lưu NVS |
| Demo chưa có linh kiện | Mock repository riêng và simulator MQTT chạy thủ công, nhãn nguồn rõ ràng |

Không làm radar theo yêu cầu mới nhất. Chưa đưa SpO₂, nhịp thở, phân loại giai đoạn ngủ, chẩn đoán hay CO₂ vào phạm vi đã thực thi. Từ bản 2.3, trợ lý AI đã kết nối Ollama thật trên máy phát triển; xem `AI.md`. Trong bảng đề xuất ban đầu có “nồng độ ôxi”; nếu thầy bắt buộc phần này ngoài PDF gốc, cần xác nhận đó là **O₂ trong không khí** hay **SpO₂ tiếp xúc trên người**, bổ sung cảm biến riêng và kiểm chứng. Bộ SHT31/BH1750 hiện tại không đo hai chỉ số đó.

## Đoạn mô tả IoT để đưa vào bảng nhóm

> Xây dựng hệ thống phòng ngủ thông minh gồm ESP32 gateway thu thập nhiệt độ, độ ẩm từ SHT31 và độ rọi từ BH1750; gửi dữ liệu qua MQTT lên máy chủ để ứng dụng Android hiển thị, lưu lịch sử và đối chiếu với từng phiên ngủ. Gateway giao tiếp ESP-NOW với ESP32 điều khiển đèn, hỗ trợ giảm sáng khi bắt đầu ngủ, tăng sáng theo lịch thức dậy và điều khiển thủ công có phản hồi. Nút nhấn tại giường ghi bắt đầu/kết thúc phiên; hệ thống có bộ nhớ đệm khi mất kết nối và đồng bộ chống trùng khi mạng trở lại. Phiên bản này không dùng radar.

## Demo khoảng 7–10 phút

1. **Yêu cầu cơ bản**: cho thấy 5 tab; thêm phiên qua đêm, ghi cảm nhận, mở lịch sử và trung bình tuần/tháng.
2. **Dữ liệu thật**: chuyển chế độ, chỉ rõ thiết bị đang kết nối và timestamp. Che BH1750 để lux đổi; quan sát nhiệt/ẩm phản hồi chậm tự nhiên, không hơ nóng linh kiện.
3. **Điều khiển hai chiều**: đặt đèn20% rồi70%; app nhận ACK và số báo độ sáng đổi.
4. **Tự động hóa**: đặt thời gian chuyển1phút và giờ thức dậy gần hiện tại; đặt đèn50%, bắt đầu ngủ để quan sát giảm sáng. Sau đó thử lịch bình minh riêng, tránh hai kịch bản trùng giờ.
5. **Nút vật lý**: kết thúc phiên đang theo dõi bằng nút, chờ đồng bộ và mở chi tiết.
6. **Ngoại tuyến**: dừng server khoảng2phút, bấm một phiên bắt đầu/kết thúc; chạy server lại, quan sát hàng đợi giảm và phiên không bị trùng. Không chạy simulator cùng lúc.
7. **Giới hạn đo lường**: nói rõ cảm biến phòng và thời gian người dùng ghi không đo các giai đoạn ngủ hoặc SpO₂.

## Phân chia theo bảng thành viên đã gửi

| Thành viên | Phần phụ trách gợi ý trong mã nguồn |
|---|---|
| Võ Quang Vinh | `ui/sleep`, luồng Start/Wake, SQLite phiên ngủ |
| Tô Quang Hiếu | `ui/home`, dashboard, trung bình/mục tiêu |
| Lê Thành Luân | RatingDialog, `ui/stats`, phân bố cảm nhận |
| Dương Nhất Vinh | `ui/history`, chi tiết/sửa/xóa, truy vấn môi trường theo phiên |
| Nguyễn Đăng Doanh | `ui/environment`, firmware gateway/lamp, MQTT và đấu nối |

Các phân công là gợi ý tổ chức bàn giao theo ảnh, chưa thay đổi tài liệu hay gửi thông báo cho thành viên.
