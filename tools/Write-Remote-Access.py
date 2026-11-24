"""Create a private local connection card; never serve these credentials through the tunnel."""
import base64,html,json,pathlib
root=pathlib.Path(__file__).resolve().parent.parent
folder=root/'backend/data'
c=json.loads((folder/'remote-access.json').read_text(encoding='utf-8-sig'))
code='sleep1.'+base64.urlsafe_b64encode((c['url']+'\n'+c['appKey']).encode()).decode().rstrip('=')
document='''<!doctype html><html lang="vi"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Kết nối Sleep</title>
<style>body{font:16px system-ui;background:#eff7fb;color:#20324a;max-width:760px;margin:40px auto;padding:24px}section{padding:24px;background:white;margin:20px 0;border-radius:12px}textarea{box-sizing:border-box;width:100%;min-height:100px;font:14px monospace;padding:12px}code{word-break:break-all}h1{font-size:26px}</style>
<h1>Kết nối Sleep từ xa</h1><p>Giữ máy tính bật, không Sleep, có Internet. Điện thoại và XIAO có thể dùng các mạng khác nhau.</p>
<section><h2>Điện thoại</h2><p>Cài APK mới, vào Cài đặt → Kết nối từ xa, dán mã dưới đây và bấm Kết nối. Chỉ gửi mã này cho điện thoại của bạn.</p><textarea readonly>{{PHONE_CODE}}</textarea></section>
<section><h2>XIAO</h2><p>Giữ BOOT 3 giây, kết nối Sleep-Setup-XXXXXX (mật khẩu Sleep-XXXXXX, cùng sáu ký tự cuối) rồi mở http://192.168.4.1. Điền Wi-Fi 2.4 GHz tại nơi đặt thiết bị.</p><p>Địa chỉ máy chủ: <code>{{SERVER_URL}}</code></p><p>Khóa thiết bị: <code>{{DEVICE_KEY}}</code></p><p>Khóa này chỉ dùng cho thiết bị, không dùng để kết nối app.</p></section>
<p>Địa chỉ miễn phí này giữ nguyên khi tunnel còn chạy; nếu tạo tunnel mới, mở lại Start-Remote.cmd và cập nhật mã/địa chỉ trên điện thoại, XIAO. Không cần mở cổng router.</p></html>'''
document=document.replace('{{PHONE_CODE}}',html.escape(code)).replace('{{SERVER_URL}}',html.escape(c['url'])).replace('{{DEVICE_KEY}}',html.escape(c['deviceKey']))
(folder/'Ket-noi-tu-xa.html').write_text(document,encoding='utf-8')
print('Private connection card: backend/data/Ket-noi-tu-xa.html')
