import { defaultSettings } from './config.js';
export class Problem extends Error { constructor(status,message){super(message);this.status=status;} }
export const assert = (condition, message, status=400) => {if(!condition)throw new Problem(status,message);};
export function checkTime(v){return Number.isSafeInteger(v) && v>=1577836800000 && v<=Date.now()+60000;}
export function validateSession(s){
  assert(typeof s.id==='string' && /^[A-Za-z0-9_-]{1,100}$/.test(s.id),'ID phiên không hợp lệ');
  assert(checkTime(s.start),'Giờ bắt đầu không hợp lệ');
  assert(s.end===null || (checkTime(s.end) && s.end>s.start && s.end-s.start<=48*3600000),'Phiên phải dài hơn 0 và không quá 48 giờ');
  assert(Number.isInteger(s.quality) && s.quality>=0 && s.quality<=5,'Đánh giá phải từ 0 đến 5');
  assert(typeof s.notes==='string' && s.notes.length<=2000,'Ghi chú tối đa 2000 ký tự');
  assert(Number.isInteger(s.version) && s.version>=0,'Phiên bản không hợp lệ');
}
export function validateSettings(s){
  const keys=Object.keys(defaultSettings);assert(keys.every(k=>typeof s[k]===typeof defaultSettings[k]),'Thiếu cấu hình');
  for(const k of keys)if(typeof s[k]==='number')assert(Number.isFinite(s[k]),'Giá trị không hợp lệ');
  assert(s.temperatureMin>=-20 && s.temperatureMax<=60 && s.temperatureMin<s.temperatureMax,'Ngưỡng nhiệt độ sai');
  assert(s.humidityMin>=0 && s.humidityMax<=100 && s.humidityMin<s.humidityMax,'Ngưỡng độ ẩm sai');
  assert(s.lightMax>=0 && s.lightMax<=65535,'Ngưỡng ánh sáng sai');
  assert(Number.isInteger(s.alarmMinute) && s.alarmMinute>=0 && s.alarmMinute<1440,'Giờ báo thức sai');
  assert(Number.isInteger(s.fadeMinutes) && s.fadeMinutes>=1 && s.fadeMinutes<=60,'Thời gian tăng sáng 1–60 phút');
  assert(Number.isInteger(s.manualBrightness) && s.manualBrightness>=0 && s.manualBrightness<=100,'Độ sáng 0–100');
  assert(Number.isInteger(s.timezoneOffset) && s.timezoneOffset>=-720 && s.timezoneOffset<=840,'Múi giờ sai');
  return Object.fromEntries(keys.map(k=>[k,s[k]]));
}
export function createStore(db,deviceId){
  const get = key => {const r=db.prepare('SELECT value FROM kv WHERE key=?').get(key);return r?JSON.parse(r.value):null;};
  const set = (key,value) => db.prepare('INSERT OR REPLACE INTO kv VALUES(?,?)').run(key,JSON.stringify(value));
  const active=()=>db.prepare('SELECT * FROM sessions WHERE end IS NULL AND deleted=0').get();
  return {db,get,set,active,
    settings:()=>({...defaultSettings,...get('settings')}),
    sessions:()=>db.prepare('SELECT * FROM sessions ORDER BY start DESC').all(),
    saveSession(s){
      validateSession(s);const old=db.prepare('SELECT * FROM sessions WHERE id=?').get(s.id);
      // Safe retry after an HTTP response was lost: identical content is already committed.
      if(old && old.start===s.start && old.end===s.end && old.quality===s.quality && old.notes===s.notes && !!old.interrupted===!!s.interrupted && !!old.deleted===!!s.deleted)return old;
      assert((old?.version||0)===s.version,'Dữ liệu đã thay đổi trên thiết bị khác. Tải lại để đối chiếu.',409);
      const a=active();assert(s.deleted || s.end!==null || !a || a.id===s.id,'Đã có một phiên đang theo dõi',409);
      const version=(old?.version||0)+1;
      db.prepare(`INSERT OR REPLACE INTO sessions VALUES(?,?,?,?,?,?,?,?,?,?)`).run(s.id,s.start,s.end,s.quality,s.notes,s.interrupted?1:0,deviceId,version,s.deleted?1:0,Date.now());
      return db.prepare('SELECT * FROM sessions WHERE id=?').get(s.id);
    },
    ingest(kind,p){
      assert(typeof p.id==='string' && /^[A-Za-z0-9_-]{1,120}$/.test(p.id),'ID gói tin sai');
      assert(checkTime(p.timestamp),'Chưa đồng bộ thời gian hoặc thời gian sai');
      assert(p.source==='device' || p.source==='simulator','Nguồn dữ liệu sai');
      if(kind==='telemetry'){
        for(const [k,min,max] of [['temperature',-40,85],['humidity',0,100],['light',0,150000]]){
          assert(p[k]===null || (typeof p[k]==='number' && Number.isFinite(p[k]) && p[k]>=min && p[k]<=max),'Số đo '+k+' sai');
        }
        for(const [name,max] of [['lightAdc',4095],['lightMillivolts',3300]]){
          assert(p[name]==null || (Number.isInteger(p[name]) && p[name]>=0 && p[name]<=max),'Số đo '+name+' sai');
        }
        db.prepare('INSERT OR IGNORE INTO telemetry(id,deviceId,timestamp,temperature,humidity,light,source,lightAdc,lightMillivolts) VALUES(?,?,?,?,?,?,?,?,?)')
          .run(p.id,deviceId,p.timestamp,p.temperature,p.humidity,p.light,p.source,p.lightAdc??null,p.lightMillivolts??null);
      }else{
        const kinds=['sleep_start','sleep_end','light_change','environment_outside','sensor_error','clock_unsynced','queue_full'];
        assert(kinds.includes(p.kind),'Loại sự kiện sai');
        if(db.prepare('SELECT id FROM events WHERE id=?').get(p.id))return;
        db.exec('BEGIN');
        try {
          db.prepare('INSERT INTO events VALUES(?,?,?,?,?,?)').run(p.id,deviceId,p.timestamp,p.kind,String(p.value||'').slice(0,1000),p.source);
          const a=active();
          if(p.kind==='sleep_start' && !a && !db.prepare('SELECT id FROM sessions WHERE start>=? AND deleted=0').get(p.timestamp)){
            db.prepare('INSERT INTO sessions VALUES(?,?,NULL,0,?,0,?,1,0,?)').run('device_'+p.id,p.timestamp,'Bắt đầu bằng nút trên thiết bị',deviceId,Date.now());
          }
          if(p.kind==='sleep_end'){
            // Reconstruct a completed offline button pair even if a newer app session exists.
            const start=db.prepare("SELECT * FROM events WHERE kind='sleep_start' AND timestamp<? ORDER BY timestamp DESC LIMIT 1").get(p.timestamp);
            const earlierEnd=start && db.prepare("SELECT id FROM events WHERE kind='sleep_end' AND timestamp>? AND timestamp<?").get(start.timestamp,p.timestamp);
            const deviceSession=start && db.prepare('SELECT * FROM sessions WHERE id=?').get('device_'+start.id);
            if(a && p.timestamp>a.start && p.timestamp-a.start<=48*3600000){
              db.prepare('UPDATE sessions SET end=?,version=version+1,updatedAt=? WHERE id=?').run(p.timestamp,Date.now(),a.id);
            }else if(start && !earlierEnd && !deviceSession && p.timestamp-start.timestamp<=48*3600000){
              db.prepare('INSERT INTO sessions VALUES(?,?,?,?,?,?,?,?,?,?)').run('device_'+start.id,start.timestamp,p.timestamp,0,'Đồng bộ phiên từ nút nhấn ngoại tuyến',0,deviceId,1,0,Date.now());
            }
          }
          db.exec('COMMIT');
        }catch(e){db.exec('ROLLBACK');throw e;}
      }
    },
    detail(start,end){
      assert(checkTime(start) && Number.isSafeInteger(end) && end>start && end<=Date.now()+60000,'Khoảng thời gian sai');
      return {samples:db.prepare('SELECT * FROM telemetry WHERE deviceId=? AND timestamp>=? AND timestamp<=? ORDER BY timestamp').all(deviceId,start,end),
        events:db.prepare('SELECT * FROM events WHERE deviceId=? AND timestamp>=? AND timestamp<=? ORDER BY timestamp').all(deviceId,start,end)};
    }
  };
}
