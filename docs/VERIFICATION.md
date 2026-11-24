# Kiểm chứng và giới hạn bàn giao

## 4.2.0 — HTTPS và cá nhân hóa, 22/11/2026

- 16 unit test Android, 5 instrumentation test điều hướng và 24 test backend đạt. Cài cập nhật bằng `adb install -r`; không gỡ app, không xóa dữ liệu.
- Ghép bằng mã trong Cài đặt đã xác nhận HTTPS thực tế. So sánh SQLite trước/sau: giữ nguyên nhật ký live và tin nhắn hiện có; hội thoại được sao chép khi xác minh cùng serverId.
- Gateway public: không có khóa trả 401, khóa thiết bị đọc nhật ký trả 403, khóa điện thoại đúng đọc được nhật ký. Kiểm thử thêm giới hạn body và NDJSON không đợi toàn bộ phản hồi.
- Gemini thật qua HTTPS đã trả lời theo tổng hợp bảy ngày, `contextUsed=true`; lượt đo nhận frame đầu khoảng 5,94 giây, hoàn tất 10,80 giây. Đây là một lượt quan sát, không phải cam kết độ trễ.
- Thử ngay trong Android qua URL public: Gemini trả lời đúng theo tổng số phút trong nhật ký thử nghiệm. Lượt đầu dịch vụ trả lỗi upstream; nút Thử lại hoàn tất thành công. Nhãn Gemini/sẵn sàng và các cảnh báo cố định không còn trong UI; lỗi thực tế vẫn hiển thị khi cần.
- XIAO đã gửi SHT30 và ADC thật qua HTTPS khi thiết bị và máy chủ sử dụng hai mạng khác nhau. Sau đó máy tính đổi mạng, tunnel tạm hết hiệu lực (`Tunnel not found`); launcher đã tạo lại và cập nhật mã kết nối.
- Theo yêu cầu sau cùng, XIAO đã lưu cấu hình Wi-Fi và URL mới, nạp thành công qua USB. Chẩn đoán thấy mạng Wi-Fi kênh 3, RSSI khoảng −81 dBm, gặp lỗi đăng nhập/rớt mạng. Chủ thiết bị chưa thể di chuyển board để thử gần router, nên chưa xác nhận được số đo mới qua mạng Wi-Fi này. App phải thể hiện offline, không coi mẫu cũ là trực tiếp.
- Firmware cuối dùng 47.076 byte RAM và 948.705 byte flash. HTTPS kiểm tra CA; không bỏ qua chứng chỉ. Chẩn đoán USB chỉ in SSID, trạng thái, kênh/RSSI, không in khóa máy chủ/mật khẩu.
- ZIP kiểm tra không chứa backend/data, secrets.h, cấu hình .env, cache/build hoặc các khóa remote/Gemini. APK đóng gói trùng build 4.2.0 đã cài.
- Chưa kiểm thử trên một điện thoại vật lý dùng 4G/5G; Android emulator đã truy cập endpoint Internet. Chưa cấu hình domain cố định, tài khoản đa người, OTA hoặc uptime thương mại.

Các mục bên dưới là kết quả phiên bản cũ, không thay thế phạm vi hiện tại ở trên.


## Bổ sung bản 2.4 — 21/11/2026

- Ollama trả nội dung dần qua `/api/ai/chat/stream` tới bong bóng đang viết trên Android. Giữ endpoint JSON cũ; rút gọn chỉ dẫn, giữ model trong RAM tối đa 15 phút sau lượt dùng. Không đổi model hoặc quyền chia sẻ dữ liệu.
- 12 test backend PASS, 4 unit test Kotlin PASS, 3 instrumentation test `AssistantStreamTest` PASS trên Pixel 3/API 36. Kiểm tra gồm nhận đoạn đầu trước khi kết thúc, UTF-8 chia giữa ký tự, thiếu sự kiện hoàn tất, lỗi giữa chừng, auth và ngắt upstream khi client hủy. Build/cài APK 2.4 (versionCode 6) thành công.
- Hai lượt cùng câu hỏi ngắn, không lịch sử: thấy đoạn đầu ở backend sau 1,074 và 0,167 giây; hoàn tất sau 15,533 và 12,365 giây. Lượt thứ hai dùng lại prompt cache. Baseline JSON trước sửa là 7,32 giây, câu trả lời ngắn hơn. Đây là cải thiện thời gian thấy nội dung, không phải bằng chứng tăng tốc tổng thời gian hay bảo đảm mọi lượt đều dưới 2 giây.
- Lượt thực tế trong app có lịch sử dài cần xử lý nhiều hơn: quan sát được bong bóng chưa hoàn tất và nhãn “Đang viết…” sau 18,28 giây tính từ lúc helper gửi xong thao tác. Đây là mốc quan sát UI, không phải phép đo chính xác token đầu tiên. Dừng làm biến mất nội dung tạm và hiện Thử lại; thử lại hoàn tất thành công. Không xóa hội thoại, bản nháp hoặc đổi quyền chia sẻ. Không có lỗi AndroidRuntime trong lượt kiểm tra.
- Câu hỏi với ngữ cảnh tổng hợp 450 phút ngày 19/11 nhận phản hồi thật sau 16,504 giây: đúng 450 phút, phân biệt thời gian tự ghi với ngủ sinh lý và nêu nguồn demo/giả lập. Không bảo đảm độ chính xác chung của model.
- Log và số đo ở `test-results/v2.4`; ảnh UI đang viết/dừng ở `screenshots/v2.4`. Model vẫn chạy CPU; lịch sử dài, nạp model lần đầu và tải máy ảnh hưởng thời gian chờ. ZIP loại trừ cấu hình riêng, database và model.

## Bổ sung bản 2.3 — 21/11/2026

- Cài Ollama 0.40.16 trên máy phát triển; cấu hình riêng `backend/.env` dùng `qwen3:4b-instruct`. Model tải từ registry Ollama, không phải fixture. Các model đã thử nhưng không chọn được gỡ để tránh giữ thêm dung lượng.
- API trạng thái kiểm tra Ollama đang chạy và model đã cài. App hiển thị nguyên nhân khi thiếu model/dịch vụ. Thời gian chờ chat tăng để phù hợp xử lý CPU; hủy yêu cầu vẫn ngắt upstream.
- 9 test backend PASS, bao gồm trạng thái thiếu dịch vụ/model, ngữ cảnh được phép, payload không yêu cầu suy luận và từ chối câu trả lời bị cắt. 4 unit test Kotlin PASS; APK 2.3 (versionCode 5) build/cài thành công.
- Hai câu hỏi qua backend tới model thật trả HTTP 200 trong 35,712 và 25,936 giây. Với dữ liệu kiểm thử, câu hỏi về ngày 19/11 trả đúng 450 phút, xác nhận là thời gian tự ghi và phân biệt dữ liệu cảm biến phòng. Đây là kiểm chứng kết nối và hai tình huống cụ thể, không phải đánh giá y khoa hay bảo đảm độ chính xác chung.
- Đã gửi thêm câu hỏi thói quen trực tiếp trong app 2.3 với chia sẻ dữ liệu tắt: nhận câu trả lời tiếng Việt của model đã chọn, hiển thị `Ollama · Sẵn sàng`; thời gian xử lý trên model khoảng 16 giây. Không có lỗi AndroidRuntime trong lượt thử. Ảnh thật ở `screenshots/v2.3/assistant-real.png`.
- Log build/test và kết quả model với dữ liệu tổng hợp lưu ở `test-results/v2.3`. Cấu hình `.env`, model và dữ liệu người dùng không đưa vào gói ZIP.

## Bổ sung bản 2.2 — 20/11/2026

- Bỏ chữ `sleep monitor` trên thanh đầu và thay tên ở chân trang Cài đặt bằng số phiên bản. Giữ tiêu đề `Sleep AI` ở màn hình trợ lý.
- Build/cài APK 2.2 (versionCode 4) thành công; 4 unit test Kotlin PASS và lượt VisualReviewTest qua các màn hình chính/keyboard PASS. Log được lưu ở `test-results/v2.2`.
- Video giới thiệu quay trực tiếp ứng dụng trên Android Emulator, dùng nguồn dữ liệu trải nghiệm hiện có. Không tạo hoặc xóa nhật ký để quay; thao tác thanh sáng chỉ xem trước, không gửi lệnh đến phần cứng.
- Giọng đọc tổng hợp nam tiếng Việt `vi-VN-NamMinhNeural`. Đã đối chiếu đầy đủ nội dung trả về từ TTS với kịch bản, không dùng đoạn âm thanh trả về thiếu câu. Lời đọc không khẳng định AI đã kết nối hoặc dữ liệu cảm biến đang lấy từ phần cứng thật.
- Kịch bản trong `demo/Loi-thuyet-minh.txt`; video và phụ đề giao riêng ở thư mục `outputs`, cùng cấp gói mã nguồn ZIP.

## Bổ sung bản 2.1 — 20/11/2026

- Build/cài APK 2.1 (versionCode 3) thành công. 4 unit test Kotlin PASS; lượt review UI trên Pixel 3/API 36 đi qua Tổng quan, Môi trường, Thống kê, Lịch sử, Ghi ngủ, Cài đặt và Sleep AI PASS. Ảnh chụp thật/video ở `screenshots/v2.1`, log ở `test-results/v2.1`.
- Không còn chuỗi “minh họa” trong mã giao diện Android. Banner nguồn đã bỏ; cấu hình nguồn và giải thích dữ liệu mẫu ở Cài đặt. Demo/live vẫn tách biệt và ngữ cảnh AI vẫn giữ nhãn nguồn đúng. Không đổi dữ liệu mẫu thành dữ liệu thiết bị thật.
- Chỉ dọn ghi chú mặc định “Dữ liệu mẫu để khám phá ứng dụng” ở 7 ID seed; các phiên khác và ghi chú đã được người dùng sửa không bị đụng tới.
- Nền cực quang, ánh sao, quỹ đạo, viền kính, halo ánh sáng và chuyển tab được dựng bằng Compose. Các gradient/path lớn được giữ trong layer, chuyển động liên tục đọc state ở lớp vẽ/layer. Tôn trọng tắt hiệu ứng và lifecycle.
- Đã đo frame trên máy ảo cả khi ghi màn hình và khi không ghi. Screen recorder gây thêm tải; kết quả này không thay thế đo pin/nhiệt độ hoặc hiệu năng trên điện thoại thật. Không cam kết một mức FPS cho mọi thiết bị.


## Bổ sung bản 2.0 — 20/11/2026

- Build APK 2.0 thành công; cài và chạy trên Android Emulator Pixel 3/API 36. Nhật ký, môi trường, thống kê, cài đặt và chatbot đã kiểm tra bằng ảnh từ ứng dụng chạy thật.
- 4 unit test Kotlin PASS. 6 instrumentation test Android PASS: các luồng cũ, context 7 ngày loại ghi chú/ID/phiên xóa, chat qua HTTP thật với **provider giả lập có nhãn kiểm thử**, cấp quyền chia sẻ, thử lại khi HTTP 429, hủy yêu cầu đang chờ và xóa hội thoại.
- 1 lượt review UI bổ sung chụp toàn bộ màn hình và kiểm tra ô gửi khi bàn phím mở. Sửa tương phản chữ ở thẻ nền tối sau khi xem ảnh thực tế.
- 8 test Node PASS, gồm 6 test mới cho Responses payload `store:false`, whitelist dữ liệu, role/size validation, trạng thái chưa cấu hình, lỗi provider, timeout/abort, giới hạn tần suất và endpoint HTTP xác thực đến fixture Ollama.
- npm kiểm tra 121 package, 0 vulnerabilities tại thời điểm chạy. Log mới trong `test-results/v2`; các kết quả firmware bản 1 bên dưới vẫn áp dụng vì firmware không thay đổi.
- **Chưa thử model AI thật**: chưa có khóa OpenAI/model Ollama cấu hình trên máy. Fixture chỉ chứng minh đường truyền, payload và trạng thái lỗi; không chứng minh chất lượng tư vấn của mô hình. Server bàn giao để AI chưa cấu hình, không giữ fixture chạy.
- Bộ SVG Figma trong `figma-reference` là bản thiết kế 1.0 cũ; giao diện 2.0 thực tế nằm trong `screenshots/v2` và mã Compose.

## Kiểm chứng hệ thống bản 1

Ngày kiểm chứng: **19/11/2026**, Windows, Node24.13.0, Android Emulator Pixel3/API36, JDK của Android Studio, PlatformIO6.1.19 với Espressif326.13.0 / Arduino-ESP322.0.17.

## Kết quả đã chạy

| Kiểm tra | Kết quả / bằng chứng |
|---|---|
| Android debug APK | Build thành công, cài và mở trên emulator; `test-results/android-build.log` |
| 4 unit test Kotlin | PASS: tính qua đêm, cộng nhiều phiên cùng ngày, bỏ phiên xóa/đang ngủ, dữ liệu cảm biến cũ/thiếu; `test-results/android-unit-tests.xml` |
| UI Start/Wake/rating/history/statistics/environment | PASS trong instrumentation chạy thật trên emulator; `test-results/android-device-test.log` |
| SQLite tách demo/live và mở lại | PASS trong instrumentation |
| Android → máy chủ REST thật | PASS: lưu offline, đồng bộ, version, tombstone; server localhost, không dùng fake HTTP |
| UI nhập/sửa/xóa và kiểm tra ngưỡng | PASS: thêm phiên8h, sửa ghi chú, xóa có xác nhận, vô hiệu hóa lưu ngưỡng không phải số; kết quả lượt bổ sung `test-results/android-extra-device-test.log` |
| 2 integration test Node | PASS: HTTP, TCP MQTT, xác thực, dedup, ACK đèn, xung đột, replay offline và database sau restart; `test-results/backend-tests.log` |
| npm audit dependency runtime | 0 vulnerabilities tại thời điểm kiểm tra |
| Firmware gateway | PlatformIO SUCCESS; RAM48,012/327,680byte; chương trình1,079,473/1,310,720byte |
| Firmware lamp | PlatformIO SUCCESS; RAM45,200/327,680byte; chương trình827,049/1,310,720byte |
| LittleFS image | Build thành công1,441,792byte; `test-results/filesystem-build.log` |
| Thuật toán C++ chạy native | PASS13 assertions: bình minh qua0giờ, giảm sáng, thức dậy, manual override, kích hoạt hôm sau |
| Kiểm tra giao diện | Ảnh chụp từ app chạy thật trong `screenshots`; không phải ảnh AI |

Firmware compilation và test thuật toán **không thay thế kiểm tra điện tử trên bo**. Máy hiện chỉ có các cổng COM Bluetooth, chưa thấy ESP32 USB để nạp hoặc đo cảm biến. Không tuyên bố đã thử ESP-NOW qua không khí, PWM trên LED, sai số SHT31/BH1750 hay mất nguồn vật lý.

## Việc kiểm tra trên phần cứng sau khi đấu

| Thử trên bàn | Điều kiện đạt |
|---|---|
| Hai cảm biến I²C | Số đo hợp lý; che BH1750 làm lux giảm; tháo một cảm biến thì giá trị thiếu/null, không báo số giả |
| Ghép cặp ESP-NOW | Đúng MAC/key/channel; lệnh10%/70% đổi LED và app nhận ACK |
| Mất kết nối đèn | Gateway vẫn online, app không báo lệnh đèn thành công khi không có ACK |
| Nút nhấn | Mỗi lần nhấn một sự kiện; không tạo trùng phiên sau reconnect |
| Mất server2–5phút | Queue tăng, khôi phục giảm; DB không có ID trùng; mẫu hiện tại không lùi về mẫu cũ |
| Tắt nguồn gateway khi còn queue | Các tệp đã flush còn sau khởi động, không auto-format; đồng bộ giờ trước khi ghi mới |
| Router mất/đổi kênh | Xác nhận fallback kênh; cấu hình lại nếu router không giữ kênh cố định |
| Bộ nhớ đệm gần đầy | Xác định số bản ghi thực tế theo LittleFS; thiết bị log lỗi, không xóa lịch sử cũ để giả vờ nhận đủ |
| Chạy qua đêm | Theo dõi reset, heap, nóng nguồn, tần suất flash, độ trễ và tính đúng lịch ít nhất một đêm |

## Giới hạn sản phẩm hiện tại

- Một người dùng/một phòng, máy chủ LAN tự chạy; chưa triển khai dịch vụ Internet.
- Firmware dành ESP32-WROOM-32. Không tự chuyển binary cho C3/S3.
- App sync khi đang mở; khi đóng, ghi cục bộ chờ lần mở sau. Gateway và server vẫn hoạt động riêng.
- Lịch hằng ngày phụ thuộc đồng hồ gateway đúng. Sau mất điện hoàn toàn phải sync giờ lại vì chưa có RTC.
- Bộ đệm hữu hạn; số file có thể chạm giới hạn dung lượng trước mức512. Không bảo đảm giữ đủ nhiều đêm ngoại tuyến.
- ACK của lamp xác nhận PWM được áp; chưa có cảm biến dòng để phát hiện LED đứt.
- Số đo môi trường, thời gian do người dùng ghi, biểu đồ và cảm nhận không tạo ra SpO₂, giai đoạn ngủ hoặc chẩn đoán.

Các kiểm thử ghi vào database thử, không nằm trong APK. Bản đóng gói loại `backend/data`, `.pio`, `node_modules`, cache Gradle và đường dẫn SDK cá nhân. Kèm lockfile npm, Gradle wrapper và phiên bản PlatformIO để nhóm dựng lại.

## Sửa điều hướng 4.0.1 — 22/11/2026

Tái hiện lỗi Sleep AI → Gợi ý cho tôi → Mở Lịch sử → Thống kê → Tổng quan: trước sửa tab Tổng quan không được chọn. Đã dùng chung điều hướng tab và quay thẳng về Home trên back stack. Kiểm tra trực tiếp bằng instrumentation `TabNavigationTest`: 4/4 đạt; unit test 14/14 đạt. Cài đè APK versionCode 12. Nhật ký không bị sửa bởi các bài kiểm tra.