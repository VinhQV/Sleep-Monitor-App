import express from 'express';
import net from 'node:net';
import { Aedes } from 'aedes';
import { randomUUID, timingSafeEqual } from 'node:crypto';
import { openDatabase } from './database.js';
import { createStore, assert, validateSettings } from './domain.js';
import { createAssistant } from './assistant.js';
import { registerVoice } from './voice.js';
function same(a,b){const x=Buffer.from(String(a||'')),y=Buffer.from(String(b));return x.length===y.length&&timingSafeEqual(x,y);}
export async function startSystem(cfg){
  const db=openDatabase(cfg.database),store=createStore(db,cfg.deviceId),broker=await Aedes.createBroker();
  const root='sleep/'+cfg.deviceId+'/';
  const publish=(topic,data,retain=false)=>new Promise((resolve,reject)=>broker.publish({topic:root+topic,payload:Buffer.from(JSON.stringify(data)),qos:1,retain},e=>e?reject(e):resolve()));
  const sendConfig=()=>publish('config',{...store.settings(),sleeping:!!store.active(),version:Date.now()},true);
  broker.authenticate=(client,user,password,done)=>done(null,user===cfg.deviceId&&same(password?.toString(),cfg.devicePassword));
  broker.authorizePublish=(client,packet,done)=>done(['telemetry','event','status','command-ack'].some(t=>packet.topic===root+t)?null:new Error('Forbidden topic'));
  broker.authorizeSubscribe=(client,sub,done)=>done(null,['command','config','ack'].some(t=>sub.topic===root+t)?sub:null);
  broker.on('clientError',()=>{});broker.on('connectionError',()=>{});
  broker.on('publish',async(packet,client)=>{
    if(!client)return;
    try{
      const kind=packet.topic.slice(root.length),p=JSON.parse(packet.payload.toString());
      if(kind==='telemetry'||kind==='event'){
        store.ingest(kind,p);await publish('ack',{id:p.id,ok:true});
        if(kind==='event' && ['sleep_start','sleep_end'].includes(p.kind))await sendConfig();
      }else if(kind==='status'){
        store.set('status',{online:p.online===true,lastSeen:Date.now(),gatewayLastSeen:Date.now(),source:p.source||'device',
          lampOnline:p.lampOnline===true,brightness:Number(p.brightness||0),timeSynced:p.timeSynced===true,queue:Number(p.queue||0)});
      }else if(kind==='command-ack'){
        assert(typeof p.id==='string','Command ID sai');
        db.prepare("UPDATE commands SET state=?,detail=? WHERE id=? AND expiresAt>=?").run(p.ok?'applied':'failed',String(p.detail||''),p.id,Date.now());
      }
    }catch(e){
      // Reject a malformed packet explicitly; never block the device queue forever.
      try {const p=JSON.parse(packet.payload.toString());if(p.id)await publish('ack',{id:p.id,ok:false,error:e.message});}catch{}
    }
  });
  const app=express();app.disable('x-powered-by');app.use(express.json({limit:'64kb'}));
  app.get('/health',(_,res)=>res.json({ok:true,service:'Sleep Monitor'}));
  app.get('/api/time',(_,res)=>res.json({timestamp:Date.now()}));
  app.use('/api',(req,res,next)=>same(req.get('X-API-Key'),cfg.apiKey)?next():res.status(401).json({error:'API key không đúng'}));
  const serverIdentity=store.get('serverIdentity')||randomUUID();
  store.set('serverIdentity',serverIdentity);
  app.get('/api/identity',(_,res)=>res.json({serverId:serverIdentity}));
  const assistant=createAssistant(cfg.ai);
  registerVoice(app,{cfg,assistant,store});
  app.post('/api/device/status',(req,res,next)=>{
    try{
      assert(typeof req.body.enabled==='boolean','Thiếu trạng thái công tắc');
      const now=Date.now();
      store.set('status',{online:req.body.enabled,enabled:req.body.enabled,lastSeen:now,
        lampOnline:false,timeSynced:true,queue:0,source:'device',sensorOnly:true});
      res.json({ok:true});
    }catch(e){next(e);}
  });
  app.post('/api/device/environment',(req,res,next)=>{
    try{
      const p=req.body;
      assert(store.get('status')?.enabled!==false,'Thiết bị đã tắt',409);
      store.ingest('telemetry',{id:randomUUID(),timestamp:Date.now(),
        temperature:p.temperature,humidity:p.humidity,light:null,source:'device',
        lightAdc:p.lightAdc??null,lightMillivolts:p.lightMillivolts??null});
      const status=store.get('status')||{};
      const now=Date.now();
      const gatewayLastSeen=status.gatewayLastSeen ?? (status.sensorOnly?0:status.lastSeen||0);
      store.set('status',{...status,online:true,lastSeen:now,gatewayLastSeen,sensorOnly:true,
        lampOnline:now-gatewayLastSeen<30000 && status.lampOnline===true,
        timeSynced:true,queue:0,source:'device'});
      res.json({ok:true});
    }catch(e){next(e);}
  });
  app.get('/api/ai/status',async(_,res)=>res.json(await assistant.checkStatus()));
  app.post('/api/ai/chat',async(req,res,next)=>{
    const controller=new AbortController();
    const disconnect=()=>{if(!res.writableEnded)controller.abort();};res.on('close',disconnect);
    try{const answer=await assistant.reply(req.body,controller.signal);if(!res.destroyed)res.json(answer);}catch(e){if(!res.destroyed)next(e);}finally{res.off('close',disconnect);}
  });
  app.post('/api/ai/chat/stream',async(req,res,next)=>{
    const controller=new AbortController();
    const disconnect=()=>{if(!res.writableEnded)controller.abort();};res.on('close',disconnect);
    const send=frame=>{
      if(res.destroyed)return;
      if(!res.headersSent){res.set({'Content-Type':'application/x-ndjson; charset=utf-8','Cache-Control':'no-cache','X-Accel-Buffering':'no'});res.flushHeaders();}
      res.write(JSON.stringify(frame)+'\n');
    };
    try{
      const answer=await assistant.reply(req.body,controller.signal,text=>send({type:'delta',text}));
      if(!res.destroyed){send({type:'done',...answer});res.end();}
    }catch(e){
      if(!res.destroyed){
        if(res.headersSent){send({type:'error',status:e.status||500,error:e.status?e.message:'Lỗi máy chủ'});res.end();}
        else next(e);
      }
    }finally{res.off('close',disconnect);}
  });
  app.get('/api/sessions',(_,res)=>res.json(store.sessions()));
  app.put('/api/sessions/:id',async(req,res,next)=>{try{assert(req.params.id===req.body.id,'ID không khớp');const s=store.saveSession(req.body);await sendConfig();res.json(s);}catch(e){next(e);}});
  app.get('/api/environment',(req,res,next)=>{try{
    const end=Date.now(),start=Number(req.query.from||end-86400000);
    assert(end-start<=32*86400000 && end>start,'Lịch sử tối đa 32 ngày');
    const status=store.get('status')||{};
    res.json({...store.detail(start,end),current:db.prepare('SELECT * FROM telemetry WHERE deviceId=? ORDER BY timestamp DESC LIMIT 1').get(cfg.deviceId)||null,
      status:{...status,online:!!status.online&&end-(status.lastSeen||0)<(status.sensorOnly?15000:30000)},settings:store.settings()});
  }catch(e){next(e);}});
  app.get('/api/detail',(req,res,next)=>{try{res.json(store.detail(Number(req.query.start),Number(req.query.end)));}catch(e){next(e);}});
  app.get('/api/settings',(_,res)=>res.json(store.settings()));
  app.put('/api/settings',async(req,res,next)=>{try{store.set('settings',validateSettings(req.body));await sendConfig();res.json(store.settings());}catch(e){next(e);}});
  app.post('/api/commands',async(req,res,next)=>{try{
    const brightness=req.body.brightness;assert(Number.isInteger(brightness)&&brightness>=0&&brightness<=100,'Độ sáng 0–100');
    const status=store.get('status');assert(status?.online&&Date.now()-status.lastSeen<30000,'Thiết bị đang ngoại tuyến',409);
    const id=randomUUID(),now=Date.now();db.prepare('INSERT INTO commands VALUES(?,?,?,?,?,?)').run(id,now,now+15000,brightness,'pending','');
    await publish('command',{id,brightness,expiresAt:now+15000});res.status(202).json({id,state:'pending'});
  }catch(e){next(e);}});
  app.get('/api/commands/:id',(req,res)=>{
    db.prepare("UPDATE commands SET state='expired' WHERE state='pending' AND expiresAt<?").run(Date.now());
    const cmd=db.prepare('SELECT * FROM commands WHERE id=?').get(req.params.id);res.status(cmd?200:404).json(cmd||{error:'Không tìm thấy lệnh'});
  });
  app.use((err,req,res,next)=>res.status(err.status||500).json({error:err.type==='entity.too.large'?'Bản thu quá dài':err.status?err.message:'Lỗi máy chủ'}));
  const tcp=net.createServer(broker.handle);await new Promise((r,j)=>{tcp.once('error',j);tcp.listen(cfg.mqttPort,cfg.host,r);});
  const http=await new Promise((r,j)=>{const server=app.listen(cfg.port,cfg.host,()=>r(server));server.once('error',j);});
  await sendConfig();
  return {store,broker,httpPort:http.address().port,mqttPort:tcp.address().port,
    close:async()=>{await new Promise(r=>http.close(r));await new Promise(r=>broker.close(r));await new Promise(r=>tcp.close(r));db.close();}};
}
