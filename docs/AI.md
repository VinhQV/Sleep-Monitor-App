# Sleep AI — cấu hình và sử dụng

Trong phiên bản 4.2.0, **Sleep AI → Gợi ý cho tôi** hoạt động ngay trên điện thoại. **Giờ ngủ** so sánh giờ bắt đầu của phiên dài nhất trong hai ngày có ghi nhận gần nhất và tính trung bình theo ngày thức dậy trong 7 ngày. **Cảm nhận** dùng đánh giá sau khi thức dậy. **Phòng ngủ** so số đo mới nhất với ngưỡng đã đặt. Mỗi gợi ý dẫn tới màn hình liên quan. App phân biệt dữ liệu trải nghiệm, thiết bị thật và số đo giả lập.

Bạn có thể chọn trả lời **Ngắn gọn** hoặc **Từng bước**. Nếu bật **Gửi chủ đề khi chat**, trợ lý còn nhận tên chủ đề để chọn gợi ý sát hơn. Android lưu các lựa chọn theo kết nối và chế độ dữ liệu. Khi bật cá nhân hóa, tổng hợp nhật ký bảy ngày và môi trường phòng được đính kèm để Gemini trả lời theo dữ liệu của bạn.

Gemini nhận hướng dẫn về thao tác và giới hạn thực tế của app trong mỗi lượt. Đây là cấu hình nội dung trả lời, không phải huấn luyện lại trọng số. Câu hỏi ngoài phạm vi được hướng dẫn quay về chủ đề giấc ngủ hoặc app. Chat cần Internet, dịch vụ Node và khóa API trên máy chủ.

## Kết nối Gemini lần đầu

1. Tạo khóa từ dự án phù hợp và kiểm tra hạn mức hiện hành trong tài khoản. [Google AI Studio](https://aistudio.google.com/apikey) · [Giá và hạn mức](https://aistudio.google.com/docs/pricing#gemini-3.5-flash-lite).
2. Mở `Configure-Cloud-AI.cmd` trên máy tính, dán khóa và nhấn Enter. Công cụ dùng một câu chào để kiểm tra luồng trả lời trước khi lưu khóa vào `backend/data/gemini.key`. ZIP loại trừ thư mục này. Khóa chỉ được gửi trực tiếp đến Google qua HTTPS.
3. Trong app Android, mở **Sleep AI → Thử kết nối lại**. Những lần sau chỉ cần chạy `Start-Sleep-AI.cmd`. Khi hết hạn mức, app hiển thị thông báo để thử lại sau.

Hội thoại Gemini được lưu riêng trên điện thoại. Google nhận nội dung hội thoại, kiểu trả lời và chủ đề nếu được bật. Theo yêu cầu của chủ ứng dụng, bản 4.2 bật **Cài đặt → Trợ lý giấc ngủ → Cá nhân hóa từ nhật ký**. Gemini nhận tổng hợp bảy ngày và số đo phòng; có thể tắt lựa chọn này cho những lượt gửi tiếp theo. [Điều khoản dữ liệu gói miễn phí](https://aistudio.google.com/docs/terms#data-use-unpaid).

Kết nối qua Internet dùng **Cài đặt → Kết nối từ xa**, xem [hướng dẫn remote](REMOTE-ACCESS.md). Trong LAN, máy ảo cũng có thể dùng `http://10.0.2.2:8080`; điện thoại thật dùng IP LAN của máy chủ. Bộ khởi chạy hiện chỉ dùng máy chủ nhẹ; không khởi chạy Ollama.

## Kết nối OpenAI

Trong thư mục `backend`, sao chép `.env.example` thành `.env`. Điền trên máy tính của bạn:

```dotenv
AI_PROVIDER=openai
AI_MODEL=<model hỗ trợ Responses API trong tài khoản của bạn>
OPENAI_API_KEY=<khóa riêng của bạn>
```

Khởi động lại bằng `npm start` hoặc `tools/Start-Server.ps1`. Không nhập khóa OpenAI vào ô “Khóa API” trong app: ô đó dùng xác thực máy chủ Sleep Monitor. Không gửi khóa qua chat hoặc đưa vào Git. Tên model phải là model tài khoản có quyền dùng; chi phí tính theo nhà cung cấp.

Triển khai dùng `POST https://api.openai.com/v1/responses`, `store:false`, giới hạn 1.600 output tokens; đọc các phần `output[].content[]` loại `output_text`. Xem [OpenAI quickstart](https://developers.openai.com/api/docs/quickstart) và [Responses API](https://developers.openai.com/api/reference/resources/responses/methods/create).

## Kết nối Ollama chạy trên máy tính

Cài Ollama và một model chat phù hợp tài nguyên máy, bảo đảm model đã có trong `ollama list` và dịch vụ đang chạy. Trong `.env`:

```dotenv
AI_PROVIDER=ollama
AI_MODEL=qwen3:4b-instruct
OLLAMA_URL=http://127.0.0.1:11434
```

Tải model bằng `ollama pull qwen3:4b-instruct`, rồi khởi động lại backend. App gọi `POST /api/ai/chat/stream`; backend dùng Ollama `POST /api/chat` với `stream:true`, `think:false`, cửa sổ 4.096 token và tối đa 512 token đầu ra. Mỗi đoạn nội dung chuyển ngay đến app bằng NDJSON; chỉ sự kiện `done` mới lưu thành câu trả lời hoàn chỉnh. Lỗi/hủy sẽ bỏ nội dung đang viết và cho phép thử lại. Endpoint JSON `/api/ai/chat` vẫn tương thích bản app cũ; OpenAI qua endpoint mới hiện toàn bộ câu trả lời khi hoàn tất, chưa có streaming token.

Chỉ dẫn được rút gọn để giảm lượng xử lý; model giữ trong RAM tối đa 15 phút sau yêu cầu gần nhất để hạn chế nạp lại giữa các lượt chat. Backend chờ tối đa 120 giây cho Ollama, app chờ tối đa 150 giây giữa các lần đọc; vẫn có nút dừng. Câu trả lời bị cắt do hết giới hạn được báo lỗi thay vì lưu như đã hoàn tất. Xem [Ollama streaming](https://docs.ollama.com/api/streaming), [chat API](https://docs.ollama.com/api/chat) và [model đã chọn](https://ollama.com/library/qwen3:4b-instruct).

Máy thử nghiệm i5-12500H, RAM 16 GB, chạy CPU. Trong hai lượt đo bản 2.4 với cùng câu hỏi ngắn và không gửi lịch sử/nhật ký, đoạn đầu đến sau **1,074 giây** và **0,167 giây**; hoàn tất sau **15,533 giây** và **12,365 giây**. Lượt thứ hai được hưởng bộ nhớ đệm prompt/model. Baseline bản 2.3 cùng câu hỏi mất 7,32 giây mới hiện toàn bộ, nhưng sinh câu trả lời ngắn hơn nên không thể suy ra tốc độ sinh token tăng. Streaming cải thiện thời gian thấy nội dung, không bảo đảm giảm tổng thời gian. Lượt đầu sau khi model bị dỡ khỏi RAM, lịch sử dài hoặc tải máy cao có thể chậm hơn. Kết quả đo lưu ở `test-results/v2.4`. Model chỉ cần tải một lần; bản đang dùng chiếm khoảng 2,5 GB trên đĩa.

## Dữ liệu và quyền riêng tư

- Nút Sleep AI giữa thanh điều hướng mở trợ lý. Có câu hỏi gợi ý, lịch sử, trạng thái chờ, dừng trả lời, thử lại và xóa hội thoại.
- Khi bật **Cá nhân hóa từ nhật ký** (mặc định của bản này), mỗi yêu cầu kèm tổng thời lượng/cảm nhận theo ngày trong **7 ngày lịch gần nhất**, cộng số đo nhiệt độ/độ ẩm/ánh sáng mới nhất, thời điểm/nguồn đo và trạng thái online. ADC/mV không được đổi tên thành lux; dữ liệu offline được phân biệt với số đo hiện tại. Ghi chú nhật ký, ID thiết bị và ID phiên không được đính kèm. Nội dung bạn tự gõ vẫn được gửi.
- AI được chỉ dẫn phân biệt mẫu/giả lập/thật, không bịa SpO₂, giai đoạn ngủ hoặc quan hệ nhân quả. Chỉ dẫn không bảo đảm mọi câu trả lời đều chính xác. Đây là trợ lý thói quen, không phải công cụ chẩn đoán.
- Tắt cá nhân hóa ngừng đính kèm dữ liệu mới, không tự xóa hội thoại. Câu trả lời cũ có thể chứa thông tin đã chia sẻ và vẫn nằm trong lịch sử gửi đi; dùng nút xóa hội thoại nếu muốn bắt đầu lại. Tối đa 40 tin lưu cục bộ; gửi tối đa 12 tin, tổng 16.000 ký tự. Hội thoại tách theo máy chủ, khóa kết nối và chế độ demo/live. Chuyển URL tới cùng máy chủ được xác minh bằng serverId sẽ sao chép hội thoại sang kết nối mới.
- Máy chủ không ghi nội dung chat vào database/log. OpenAI đặt `store:false`; chính sách xử lý/lưu giữ phía nhà cung cấp vẫn áp dụng. Xóa hội thoại trong app không thu hồi dữ liệu đã gửi.
- Chỉ điều khiển đèn bằng giao diện thiết bị. Chatbot không có công cụ sửa nhật ký/điều khiển IoT.
- API yêu cầu `X-API-Key`; giới hạn chung 10 yêu cầu/phút và 2 yêu cầu đồng thời cho máy chủ một người dùng. Timeout/hủy sẽ ngắt kết nối upstream. Với Ollama, trạng thái sẵn sàng kiểm tra dịch vụ đang chạy và model có trong `/api/tags`; câu hỏi mới kiểm tra khả năng sinh câu trả lời. Với OpenAI, trạng thái chỉ kiểm tra cấu hình, chưa xác nhận quyền hoặc hạn mức tài khoản.

## Kiểm thử

`npm test` kiểm tra payload, xác thực, giới hạn, lọc dữ liệu, timeout/hủy, lỗi upstream và endpoint HTTP với provider giả lập có nhãn rõ. Có kiểm tra gửi được đoạn đầu trước khi provider hoàn tất, giải mã UTF-8 bị chia giữa ký tự, ngắt upstream khi client hủy và từ chối phản hồi bị cắt. Android `AssistantStreamTest` kiểm tra nhận nội dung trước sự kiện hoàn tất, lỗi giữa chừng và UTF-8. `node test/android-fixture.js` chỉ dành kiểm thử Android tự động trên localhost: mọi câu trả lời đều bắt đầu bằng **KIỂM THỬ KẾT NỐI — không phải câu trả lời AI**. Không dùng fixture để thuyết trình chất lượng mô hình.

Ngày 21/11/2026 đã thử model Ollama thật qua REST backend: hỏi thói quen chung và hỏi 450 phút của ngày 19/11 trong một bộ dữ liệu kiểm thử. Model trả lời đúng số được cung cấp và nói rõ đây là thời gian tự ghi, không phải giấc ngủ sinh lý do cảm biến đo. Các lượt thử này không bảo đảm mọi câu trả lời đều đúng. OpenAI vẫn chưa được cấu hình/thử với tài khoản thật.
