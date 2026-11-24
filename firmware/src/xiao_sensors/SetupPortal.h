#pragma once
#include <WiFi.h>
#include <WebServer.h>
#include <DNSServer.h>
#include <Preferences.h>
namespace SetupPortal {
WebServer web(80); DNSServer dns; Preferences prefs;
String ssid,pass,url,key,previousSsid,previousPass,token;
bool open=false,trying=false; uint32_t openedAt=0,tryAt=0,heldAt=0,closeAt=0;
String escape(String s){s.replace("&","&amp;");s.replace("<","&lt;");s.replace("\"","&quot;");return s;}
bool validServer(String u){
 return u.length()<=200&&(u.startsWith("https://")||u.startsWith("http://"))&&u.indexOf('@')<0&&u.indexOf('?')<0&&u.indexOf('#')<0&&u.indexOf(' ')<0;
}
bool configureServer(String nextUrl,String nextKey){
 nextUrl.trim();while(nextUrl.endsWith("/"))nextUrl.remove(nextUrl.length()-1);
 if(!validServer(nextUrl)||!nextUrl.startsWith("https://")||nextKey.length()<32||nextKey.length()>128)return false;
 url=nextUrl;key=nextKey;prefs.putString("url",url);prefs.putString("key",key);return true;
}
bool configureWifi(String nextSsid,String nextPass){
 if(nextSsid.isEmpty()||nextSsid.length()>32||(nextPass.length()>0&&(nextPass.length()<8||nextPass.length()>63)))return false;
 ssid=nextSsid;pass=nextPass;prefs.putString("ssid",ssid);prefs.putString("pass",pass);
 trying=false;WiFi.disconnect();WiFi.begin(ssid.c_str(),pass.c_str());return true;
}
void stop(){web.stop();dns.stop();WiFi.softAPdisconnect(true);WiFi.mode(WIFI_STA);open=false;trying=false;closeAt=0;}
void start(){
 if(open)return;
 char suffix[7];snprintf(suffix,sizeof(suffix),"%06X",unsigned(ESP.getEfuseMac()&0xFFFFFF));
 String name="Sleep-Setup-"+String(suffix);String password="Sleep-"+String(suffix);
 token=String(esp_random(),HEX)+String(esp_random(),HEX);
 WiFi.mode(WIFI_AP_STA);WiFi.softAP(name.c_str(),password.c_str());dns.start(53,"*",WiFi.softAPIP());
 static bool registered=false;
 if(!registered){registered=true;
 web.on("/",HTTP_GET,[]{
  String html="<!doctype html><html lang='vi'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>Thiết lập Sleep</title><style>body{font:16px system-ui;background:#eef8ff;color:#203345;max-width:500px;margin:30px auto;padding:20px}input,button{box-sizing:border-box;width:100%;padding:14px;margin:8px 0 20px;border:1px solid #bdd5e8;border-radius:8px}button{background:#237ca9;color:white}small{display:block}</style><h1>Kết nối phòng ngủ</h1><p>Wi-Fi 2.4 GHz. Cấu hình được lưu trên thiết bị.</p><form method='post' action='/save'><input type='hidden' name='token' value='"+token+"'><label>Tên Wi-Fi<input name='ssid' maxlength='32' required value=\""+escape(ssid)+"\"></label><label>Mật khẩu Wi-Fi<input type='password' name='pass' maxlength='63'></label><label>Địa chỉ máy chủ<input name='url' required placeholder='http://192.168.1.10:8080' value=\""+escape(url)+"\"></label><label>Khóa kết nối máy chủ<input type='password' name='key' placeholder='Để trống để giữ khóa hiện tại'></label><button>Kết nối và lưu</button></form><small>Chỉ lưu sau khi kết nối Wi-Fi thành công. Địa chỉ máy chủ cần truy cập được từ Wi-Fi mới.</small></html>";
  web.sendHeader("Cache-Control","no-store");web.send(200,"text/html; charset=utf-8",html);
 });
 web.on("/save",HTTP_POST,[]{
  if(web.arg("token")!=token){web.send(403,"text/plain","Yeu cau khong hop le");return;}
  String n=web.arg("ssid"),p=web.arg("pass"),u=web.arg("url");u.trim();while(u.endsWith("/"))u.remove(u.length()-1);
  if(n.isEmpty()||n.length()>32||(p.length()>0&&(p.length()<8||p.length()>63))||(!u.startsWith("http://")&&!u.startsWith("https://"))||u.length()>200||u.indexOf('@')>=0||u.indexOf('?')>=0||u.indexOf('#')>=0){web.send(400,"text/plain; charset=utf-8","Kiểm tra Wi-Fi, mật khẩu (8–63 ký tự), địa chỉ http:// nội bộ hoặc https:// từ xa.");return;}
  if(trying){web.send(409,"text/plain","Dang thu ket noi");return;}
  previousSsid=ssid;previousPass=pass;ssid=n;pass=p;url=u;if(web.arg("key").length())key=web.arg("key");
  WiFi.disconnect();WiFi.begin(ssid.c_str(),pass.c_str());trying=true;tryAt=millis();
  web.send(200,"text/html; charset=utf-8","<meta charset='utf-8'><meta name='viewport' content='width=device-width'><h2>Đang thử kết nối…</h2><p id='s'>Vui lòng chờ tối đa 25 giây.</p><script>setInterval(async()=>{try{let r=await fetch('/status');document.getElementById('s').textContent=await r.text()}catch{}},2000)</script>");
 });
 web.on("/status",HTTP_GET,[]{web.send(200,"text/plain; charset=utf-8",trying?"Đang kết nối Wi-Fi…":(closeAt?"Đã lưu Wi-Fi. Kết nối điện thoại lại Wi-Fi nhà bạn; thiết bị sẽ gửi dữ liệu tới máy chủ đã chọn.":"Kết nối chưa thành công. Về trang chính để kiểm tra lại mật khẩu."));});
 web.onNotFound([]{web.sendHeader("Location","http://192.168.4.1/");web.send(302,"text/plain","");});}
 web.begin();open=true;openedAt=millis();
 Serial.printf("Setup Wi-Fi: %s; password: %s; http://192.168.4.1 (10 min)\n",name.c_str(),password.c_str());
}
void begin(const char* defaultSsid,const char* defaultPass,const char* defaultUrl,const char* defaultKey){
 prefs.begin("sleep-wifi",false);ssid=prefs.getString("ssid",defaultSsid);pass=prefs.getString("pass",defaultPass);url=prefs.getString("url",defaultUrl);key=prefs.getString("key",defaultKey);
 pinMode(0,INPUT_PULLUP);WiFi.mode(WIFI_STA);WiFi.setSleep(false);WiFi.setAutoReconnect(true);WiFi.begin(ssid.c_str(),pass.c_str());
}
void tick(){
 if(digitalRead(0)==LOW){if(!heldAt)heldAt=millis();else if(millis()-heldAt>=3000)start();}else heldAt=0;
 if(!open)return;
 web.handleClient();dns.processNextRequest();
 if(trying&&WiFi.status()==WL_CONNECTED){prefs.putString("ssid",ssid);prefs.putString("pass",pass);prefs.putString("url",url);prefs.putString("key",key);trying=false;closeAt=millis()+8000;}
 if(trying&&millis()-tryAt>25000){ssid=previousSsid;pass=previousPass;url=prefs.getString("url",VOICE_SERVER_URL);key=prefs.getString("key",VOICE_SERVER_KEY);WiFi.disconnect();WiFi.begin(ssid.c_str(),pass.c_str());trying=false;}
 if(closeAt&&millis()>closeAt){stop();ESP.restart();return;}
 if(millis()-openedAt>600000)stop();
}
}
